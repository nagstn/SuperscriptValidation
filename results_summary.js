// --- results_summary.js ---
document.addEventListener('DOMContentLoaded', () => {
    console.log("[Results Summary] DOMContentLoaded fired.");

    const resultsContainer = document.getElementById('resultsContainer');
    const summaryDiv = document.getElementById('summary');
    const CURRENT_RESULTS_KEY = "multiPageScanResults_Current";
    const PREVIOUS_RESULTS_KEY = "multiPageScanResults_Previous";
    const STATUS_STORAGE_KEY = "multiPageScanStatus";

    // --- Helper Function for Icons ---
    function getPositionIcon(position) {
        // Keep this simple, handle potential undefined input gracefully
        switch(position){
            case 'ABOVE BASELINE': return '<span class="pos-icon pos-above" title="Above Baseline">✅</span>';
            case 'SAMELINE': return '<span class="pos-icon pos-sameline" title="Same Line">❌</span>';
            case 'BLANK': return '<span class="pos-icon pos-blank" title="Blank/Empty">⚠️</span>';
            case 'CHECK_ERROR': return '<span class="pos-icon pos-error" title="Position Check Error">❗</span>';
            default: return '<span class="pos-icon pos-unknown" title="Unknown/Invalid">❓</span>'; // Handle null/undefined/other
        }
    }

    // --- Helper Function for CSS Class ---
    function getPositionClass(position) {
        // Keep this simple, handle potential undefined input gracefully
        switch(position){
            case 'ABOVE BASELINE': return 'pos-above';
            case 'SAMELINE': return 'pos-sameline';
            case 'BLANK': return 'pos-blank';
            case 'CHECK_ERROR': return 'pos-error';
            default: return 'pos-unknown'; // Handle null/undefined/other
        }
    }

    // --- Helper Function to check if text is purely numeric ---
    function isNumeric(str) {
        if (typeof str !== 'string' || !str.trim()) return false; // Check if string and not empty/whitespace
        return /^\d+$/.test(str.trim()); // Check if it contains only digits
    }

    /**
     * Extracts the meaningful visible text from a superscript's outerHTML string OR plain text.
     * Prioritizes finding text nodes directly within the sup or its immediate children,
     * after attempting to ignore hidden elements via innerText.
     * Includes logging for diagnostics.
     *
     * @param {string} inputString The outerHTML string of the <sup> element OR potentially just plain text.
     * @returns {string} The extracted visible text, or an empty string if none found.
     */
    function extractVisibleSuperscriptText(inputString) {
      if (!inputString || typeof inputString !== 'string') {
        return '';
      }
      const trimmedInput = inputString.trim();
      // console.log(`[extractText] Raw Input: ${trimmedInput}`); // Log raw input

      // Handle cases where input is likely plain text already
      if (!trimmedInput.includes('<') && !trimmedInput.includes('>')) {
          // console.log(`[extractText] Input "${trimmedInput}" treated as plain text.`);
          // Optionally, still clean known junk text even if no tags present
          if (trimmedInput.includes('Opens a modal dialog')) {
              // Attempt to extract number following the junk text
              const match = trimmedInput.match(/Opens a modal dialog for footnote\s*(\d+)/);
              if (match && match[1]) {
                  // console.log(`[extractText] Extracted number "${match[1]}" from plain text.`);
                  return match[1];
              }
              return ''; // Return empty if no number found after junk
          }
          return trimmedInput; // Return the plain text directly
      }

      // Proceed with parsing only if it looks like HTML
      try {
        const parser = new DOMParser();
        const doc = parser.parseFromString(inputString, 'text/html');
        const supElement = doc.body.firstChild;

        // Basic validation
        if (!supElement || supElement.nodeName !== 'SUP') {
          console.warn("[extractText] Could not parse sup element from string or not a SUP:", inputString);
          // Fallback: Try regex on original string for simple <sup> tags
          const simpleMatch = inputString.match(/<sup>(.*?)<\/sup>/i);
          return simpleMatch ? simpleMatch[1].trim() : '';
        }

        let extractedText = '';

        // --- Strategy 1: Use innerText (often works for simple cases and ignores some hidden) ---
        extractedText = supElement.innerText?.trim();
        // console.log(`[extractText] Attempt 1 (innerText): "${extractedText}"`);

        // --- Strategy 2: If innerText failed or looks like it included hidden text, parse manually ---
        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
            // console.log('[extractText] Attempt 1 failed or contained hidden text, trying manual parse...');
            let potentialText = '';
            const childNodes = Array.from(supElement.childNodes);
            for (const node of childNodes) {
                if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) {
                    potentialText = node.textContent.trim();
                    break;
                } else if (node.nodeType === Node.ELEMENT_NODE) {
                    const grandChildNodes = Array.from(node.childNodes);
                    for (const grandNode of grandChildNodes) {
                        if (grandNode.nodeType === Node.TEXT_NODE && grandNode.textContent?.trim()) {
                            potentialText = grandNode.textContent.trim();
                        }
                    }
                    if (potentialText) break;
                }
            }
            if (potentialText) {
                 extractedText = potentialText;
            }
            // console.log(`[extractText] Attempt 2 (manual parse) result: "${extractedText}"`);
        }

        // --- Strategy 3: Fallback - Remove known hidden elements and get textContent ---
        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
            // console.log('[extractText] Attempts 1 & 2 failed or contained hidden text, trying node removal...');
            const clone = supElement.cloneNode(true);
            clone.querySelectorAll('.hidden, .sr-only, .visually-hidden, [aria-hidden="true"], [data-translation-text]').forEach(hiddenEl => {
                hiddenEl.remove();
            });
            let textAfterRemoval = clone.textContent?.trim() || '';
             if (textAfterRemoval) {
                 extractedText = textAfterRemoval;
             }
            // console.log(`[extractText] Attempt 3 (node removal) result: "${extractedText}"`);
        }

        // Final Log
        // console.log(`[extractText] Input: ${inputString}, Final Output: "${extractedText}"`); // Keep commented unless debugging specific cases

        return extractedText || ''; // Ensure we always return a string

      } catch (e) {
        console.error("[extractText] Error parsing outerHTML:", e, "Input:", inputString);
        return ''; // Return empty string on error
      }
    }


    // --- Main Function to Load and Display ---
    async function loadAndDisplayResults() {
        console.log("[Results Summary] loadAndDisplayResults started.");

        // Ensure summaryDiv and resultsContainer are valid at the start
        if (!summaryDiv) {
            console.error("[Results Summary] CRITICAL: summaryDiv element not found!");
            if (resultsContainer) {
                resultsContainer.innerHTML = '<div class="error-message">Page structure error: Summary element missing.</div>';
            } else {
                document.body.innerHTML = '<div class="error-message">Page structure error: Summary element missing.</div>';
            }
            return;
        }
        if (!resultsContainer) {
             console.error("[Results Summary] CRITICAL: resultsContainer element not found!");
             summaryDiv.innerHTML = '<div class="error-message">Page structure error: Results container missing.</div>';
             return;
        }

        resultsContainer.innerHTML = '<div class="loading">Loading results...</div>';
        summaryDiv.innerHTML = 'Loading summary...'; // Set initial text

        try {
            console.log("[Results Summary] Attempting to get data from storage...");
            const storedData = await chrome.storage.local.get([
                CURRENT_RESULTS_KEY,
                PREVIOUS_RESULTS_KEY,
                STATUS_STORAGE_KEY
            ]);
            console.log("[Results Summary] Data fetched from storage:", storedData);

            const currentResults = storedData[CURRENT_RESULTS_KEY];
            const previousResults = storedData[PREVIOUS_RESULTS_KEY];
            const statusInfo = storedData[STATUS_STORAGE_KEY]; // Might be undefined/null if not set

            if (!currentResults || Object.keys(currentResults).length === 0) {
                console.log("[Results Summary] No current results found in storage.");
                resultsContainer.innerHTML = '<div class="no-results">No current scan results found. Run a scan or check background logs.</div>';
                summaryDiv.textContent = 'No current scan results available.';
                return; // Exit if no results
            }

            console.log("[Results Summary] Current results found. Proceeding to display.");
            resultsContainer.innerHTML = ''; // Clear loading message for results

            // --- Calculate Summary Metrics ---
            let totalUrls = Object.keys(currentResults).length;
            let successCount = 0;
            let errorCount = 0;
            let totalItemsFound = 0;
            let urlsWithIssues = 0;

            Object.values(currentResults).forEach(result => {
                if (!result) return; // Add safety check for null/undefined results in the array

                if (result.status === 'success') {
                    successCount++;
                    if (result.data && Array.isArray(result.data) && result.data.length > 0) { // Check if data is an array
                        totalItemsFound += result.data.length;
                        // Add safety check for item being null/undefined within some()
                        if (result.data.some(item => item && (item.position === 'SAMELINE' || item.position === 'BLANK' || item.position === 'CHECK_ERROR'))) {
                            urlsWithIssues++;
                        }
                    }
                } else {
                    errorCount++;
                }
            });
            console.log("[Results Summary] Summary calculated:", { totalUrls, successCount, errorCount, totalItemsFound, urlsWithIssues });

            // --- Display Enhanced Summary ---
            console.log("[Results Summary] Preparing to update summaryDiv. Current content:", summaryDiv.innerHTML);
            try {
                 const completedTimeStr = statusInfo?.completedTime ? `<span class="scan-time">Scan completed on: ${new Date(statusInfo.completedTime).toLocaleString()}</span>` : '';
                 const runningStatusStr = statusInfo?.running ? `<span class="scan-time">Scan is currently running... Refresh for updates.</span>` : '';

                 summaryDiv.innerHTML = `
                    <span class="metric metric-total"><strong>${totalUrls}</strong> URLs Scanned</span> |
                    <span class="metric metric-success"><strong>${successCount}</strong> Succeeded</span> |
                    <span class="metric metric-error"><strong>${errorCount}</strong> Failed</span> |
                    <span class="metric metric-total"><strong>${totalItemsFound}</strong> Items Found</span> |
                    <span class="metric ${urlsWithIssues > 0 ? 'metric-error' : 'metric-success'}"><strong>${urlsWithIssues}</strong> URLs with Issues</span>
                    ${completedTimeStr}
                    ${runningStatusStr}
                `;
                console.log("[Results Summary] summaryDiv.innerHTML updated successfully.");
            } catch (summaryUpdateError) {
                 console.error("[Results Summary] ERROR occurred while updating summaryDiv.innerHTML:", summaryUpdateError);
                 summaryDiv.textContent = 'Error displaying summary. Check console.';
            }
             console.log("[Results Summary] Summary display attempt finished. Current content:", summaryDiv.innerHTML);

            // --- Display Results Per URL using Cards ---
            const sortedUrls = Object.keys(currentResults).sort();
            console.log(`[Results Summary] Processing ${sortedUrls.length} URLs.`);

            for (const url of sortedUrls) {
                const currentResultItem = currentResults[url];
                // Add safety check for currentResultItem itself
                if (!currentResultItem) {
                    console.warn(`[Results Summary] Skipping invalid result entry for URL key: ${url}`);
                    continue; // Skip to the next URL if the result item is missing/invalid
                }
                const previousResultItem = previousResults ? previousResults[url] : null;

                const cardDiv = document.createElement('div');
                cardDiv.className = 'page-results-card';

                // --- Card Header ---
                const cardHeader = document.createElement('div');
                // Add safety check for currentResultItem.data before accessing .some()
                const hasIssues = currentResultItem.data && Array.isArray(currentResultItem.data) && currentResultItem.data.some(item => item && (item.position === 'SAMELINE' || item.position === 'BLANK' || item.position === 'CHECK_ERROR'));
                const startCollapsed = currentResultItem.status === 'error' || !hasIssues;
                cardHeader.className = `card-header ${startCollapsed ? 'collapsed' : ''}`;

                // --- Header Content Wrapper ---
                const headerContent = document.createElement('div');
                headerContent.className = 'header-content';

                // --- Header Main Line ---
                const headerMainLine = document.createElement('div');
                headerMainLine.className = 'header-main-line';
                const urlHeading = document.createElement('div');
                urlHeading.className = 'page-url';
                urlHeading.textContent = url;
                const statusBadge = document.createElement('span');
                statusBadge.className = 'status-badge';
                headerMainLine.appendChild(urlHeading);
                headerMainLine.appendChild(statusBadge);

                // --- Comparison Info Line ---
                const comparisonDiv = document.createElement('div');
                comparisonDiv.className = 'comparison-info';
                // --- User Header Change ---
                let comparisonText = 'SupCount(Prev.Rel/Sprint): N/A';
                // --- End User Header Change ---
                let currentCount = 0; // Store the numeric count if possible
                let currentCountDisplay = '<strong>0</strong>'; // Display string (might be "Error")
                let detailCountsString = ''; // String for the new counts

                if (currentResultItem.status === 'success' && currentResultItem.data && Array.isArray(currentResultItem.data)) { // Check if data is array
                    currentCount = currentResultItem.data.length;
                    currentCountDisplay = `<strong>${currentCount}</strong>`;

                    // --- Calculate Detail Counts ---
                    let posAbove = 0, posSame = 0, posBlank = 0, posOther = 0;
                    let missingTrue = 0, missingFalse = 0;
                    let textNumeric = 0, textNonNumeric = 0, textBlank = 0;

                    currentResultItem.data.forEach(item => {
                        if (!item) return; // Safety check for item

                        // Position Counts
                        switch(item.position) {
                            case 'ABOVE BASELINE': posAbove++; break;
                            case 'SAMELINE': posSame++; break;
                            case 'BLANK': posBlank++; break;
                            default: posOther++; break;
                        }

                        // Missing Tag Counts
                        if (item.SupMissing === true) { // Use correct property name
                            missingTrue++;
                        } else {
                            missingFalse++; // Count false, null, undefined as false
                        }

                        // Superscript Text Type Counts
                        const extractedText = extractVisibleSuperscriptText(item.superscriptText); // Use the updated helper
                        if (extractedText === null || extractedText === undefined || extractedText.trim() === '') {
                            textBlank++;
                        } else if (isNumeric(extractedText)) {
                            textNumeric++;
                        } else {
                            textNonNumeric++;
                        }
                    });

                    // Format the counts string - REMOVE prefix spaces
                    detailCountsString = `[` + // Removed &nbsp;&nbsp;
                        `Pos: ${posAbove}(✓) ${posSame}(✗) ${posBlank}(⚠️) ${posOther}(?) | ` +
                        `Missing: ${missingTrue}(T) ${missingFalse}(F) | ` +
                        `Text: ${textNumeric}(#) ${textNonNumeric}(Ab) ${textBlank}(Bl)` +
                    `]`;
                    // --- End Calculate Detail Counts ---

                } else if (currentResultItem.status === 'error') {
                    currentCountDisplay = '<strong>Error</strong>';
                    currentCount = NaN; // Indicate current count is not a comparable number
                }
                // Handle cases where status might be neither 'success' nor 'error' or data is missing/not array
                else if (currentResultItem.status !== 'success') {
                     currentCountDisplay = '<strong>N/A</strong>'; // Or some other indicator
                     currentCount = NaN;
                }

                // --- User Header Change ---
                const currentHeaderText = `SupCount(Current.Rel/Sprint): ${currentCountDisplay}`;
                // --- End User Header Change ---

                if (previousResultItem) {
                    // Add safety check for previousResultItem.data being an array
                    if (previousResultItem.status === 'success' && previousResultItem.data && Array.isArray(previousResultItem.data)) {
                        const previousCount = previousResultItem.data.length;
                        // --- User Header Change ---
                        comparisonText = `SupCount(Prev.Rel/Sprint): <strong>${previousCount}</strong> | ${currentHeaderText}`;
                        // --- End User Header Change ---

                        // Only add indicators and PASS/FAIL if both counts are valid numbers
                        if (!isNaN(currentCount) && typeof previousCount === 'number') {
                            const change = currentCount - previousCount;
                            let changeClass = 'change-same';
                            let changeSymbol = '●'; // Default to same
                            let passFailStatus = ''; // Initialize Pass/Fail status
                            let passFailClass = '';
                            let passFailDisplay = ''; // Display string (may include strong tag)
                            let extraSpacing = ''; // Variable for extra spaces

                            if (change > 0) {
                                changeClass = 'change-increase';
                                changeSymbol = '▲';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else if (change < 0) {
                                changeClass = 'change-decrease';
                                changeSymbol = '▼';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else { // change === 0
                                changeClass = 'change-same';
                                changeSymbol = '●';
                                passFailStatus = 'PASS';
                                passFailClass = 'status-pass';
                                passFailDisplay = `<strong>${passFailStatus}</strong>`;
                                // --- ADD SPACING FOR PASS ---
                                extraSpacing = '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'; // 8 non-breaking spaces
                                // --- END ADD SPACING ---
                            }

                            // Append change indicator and PASS/FAIL status
                            comparisonText += `<span class="change-indicator ${changeClass}">${changeSymbol} ${change !== 0 ? change : ''}</span>`;
                            comparisonText += ` <span class="pass-fail-status ${passFailClass}">${passFailDisplay}</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---

                        } else {
                             // Handle cases where one count is 'Error' or invalid
                             comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                             // --- Append Detail Counts String WRAPPED in a span ---
                             comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                             // --- End Append ---
                        }

                    } else if (previousResultItem.status === 'error') {
                         // --- User Header Change ---
                         comparisonText = `SupCount(Prev.Rel/Sprint): <strong>Error</strong> | ${currentHeaderText}`;
                         // --- End User Header Change ---
                         comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                         // --- Append Detail Counts String WRAPPED in a span ---
                         comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                         // --- End Append ---
                    } else { // Previous existed but wasn't success/error...
                         const previousCount = 0; // Assume 0 for comparison
                         // --- User Header Change ---
                         comparisonText = `SupCount(Prev.Rel/Sprint): <strong>0</strong> | ${currentHeaderText}`;
                         // --- End User Header Change ---
                         if (!isNaN(currentCount)) {
                            const change = currentCount - previousCount;
                            let changeClass = 'change-same';
                            let changeSymbol = '●';
                            let passFailStatus = '';
                            let passFailClass = '';
                            let passFailDisplay = '';
                            let extraSpacing = '';

                            if (change > 0) {
                                changeClass = 'change-increase';
                                changeSymbol = '▲';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else { // change === 0
                                changeClass = 'change-same';
                                changeSymbol = '●';
                                passFailStatus = 'PASS';
                                passFailClass = 'status-pass';
                                passFailDisplay = `<strong>${passFailStatus}</strong>`;
                                extraSpacing = '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
                            }
                            comparisonText += `<span class="change-indicator ${changeClass}">${changeSymbol} ${change !== 0 ? change : ''}</span>`;
                            comparisonText += ` <span class="pass-fail-status ${passFailClass}">${passFailDisplay}</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---
                         } else {
                            comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---
                         }
                    }
                } else { // No previous result item
                     // --- User Header Change ---
                     comparisonText = `SupCount(Prev.Rel/Sprint): N/A | ${currentHeaderText}`;
                     // --- End User Header Change ---
                     // --- Append Detail Counts String WRAPPED in a span ---
                     // Ensure detailCountsString is appended even if it's empty (will just be '[]')
                     comparisonText += `<span class="detail-counts">${detailCountsString || '[]'}</span>`;
                     // --- End Append ---
                }
                comparisonDiv.innerHTML = comparisonText;


                // --- Assemble Header Content ---
                headerContent.appendChild(headerMainLine);
                headerContent.appendChild(comparisonDiv);

                // --- Toggle Icon ---
                const toggleIcon = document.createElement('span');
                toggleIcon.className = 'toggle-icon';
                toggleIcon.innerHTML = '&#9660;'; // Down arrow for toggle

                // --- Add Content and Icon to Header ---
                cardHeader.appendChild(headerContent);
                cardHeader.appendChild(toggleIcon);

                // --- Card Body ---
                const cardBody = document.createElement('div');
                cardBody.className = 'card-body';

                // --- Populate Card Body and Status Badge ---
                if (currentResultItem.status === 'error') {
                    statusBadge.classList.add('error');
                    statusBadge.textContent = 'Error';
                    const errorP = document.createElement('div');
                    errorP.className = 'error-message';
                    errorP.textContent = `Error: ${currentResultItem.error || 'Unknown error'}`;
                    cardBody.appendChild(errorP);
                } else if (currentResultItem.status === 'success' && currentResultItem.data && Array.isArray(currentResultItem.data)) { // Check data is array
                    const data = currentResultItem.data;
                    if (data.length === 0) {
                        statusBadge.classList.add('no-items');
                        statusBadge.textContent = 'No Items Found';
                        cardBody.innerHTML = '<div class="no-results">Scan successful, no superscripts found on this page.</div>';
                    } else {
                        statusBadge.classList.add('success');
                        statusBadge.textContent = `Found ${data.length}`;
                        // Call the updated createResultsTable function
                        const tableElement = createResultsTable(data, url); // Pass URL for logging context
                        if (tableElement instanceof Node) {
                            cardBody.appendChild(tableElement);
                        } else {
                            // Fallback error message
                            console.error(`[Results Summary] FALLBACK ERROR: createResultsTable did not return a Node for URL "${url}". Returned:`, tableElement);
                            const errorPlaceholder = document.createElement('div');
                            errorPlaceholder.className = 'error-message';
                            errorPlaceholder.textContent = 'Internal error: Failed to display results table (Fallback). Check console.';
                            cardBody.appendChild(errorPlaceholder);
                        }
                    }
                } else { // Handle cases where status isn't success or data is missing/not array
                    statusBadge.classList.add('error');
                    statusBadge.textContent = 'Unknown/Invalid Status';
                    const unknownP = document.createElement('div');
                    unknownP.className = 'error-message';
                    unknownP.textContent = `Unknown status ('${currentResultItem.status}') or missing/invalid data.`;
                    cardBody.appendChild(unknownP);
                }

                // --- Assemble Card ---
                cardDiv.appendChild(cardHeader);
                cardDiv.appendChild(cardBody);
                resultsContainer.appendChild(cardDiv);

                // --- Add Collapse Toggle Listener ---
                cardHeader.addEventListener('click', () => {
                    cardHeader.classList.toggle('collapsed');
                    // console.log(`Toggled collapsed class for ${url}. Header classes: ${cardHeader.className}`); // Optional logging
                });
            }
            console.log("[Results Summary] Finished processing URLs.");

        } catch (error) {
            // This is the main catch block
            console.error("[Results Summary] Caught error in loadAndDisplayResults (main try block):", error);
            if (summaryDiv) {
                 summaryDiv.textContent = 'Error loading results.';
            }
            if (resultsContainer) {
                 resultsContainer.innerHTML = `<div class="error-message">Error loading results: ${error.message}. Check console.</div>`;
            }
        }
        console.log("[Results Summary] loadAndDisplayResults finished.");
    }

    // --- Function to Create the Results Table (with internal error handling and updated headers) ---
       // --- Function to Create the Results Table ---
        // --- Function to Create the Results Table ---
    function createResultsTable(data, urlForContext = 'Unknown URL') {
        const table = document.createElement('table');
        try {
            const thead = table.createTHead();
            const tbody = table.createTBody();
            const headerRow = thead.insertRow();
            const headers = ['PreceedingText+sup', 'SuperscriptText', 'SupMissing?', 'SupPosition'];
            headers.forEach(text => { const th = document.createElement('th'); th.textContent = text; headerRow.appendChild(th); });

            if (!Array.isArray(data)) {
                 console.error(`[Comparer Window] ERROR in createResultsTable for ${urlForContext}: Input data is not an array.`, data);
                 const errorRow = tbody.insertRow();
                 const cell = errorRow.insertCell();
                 cell.colSpan = headers.length;
                 cell.textContent = "Error: Invalid data format received.";
                 cell.style.color = 'red'; cell.style.fontStyle = 'italic';
                 return table;
            }

            data.forEach((item, index) => {
                try {
                    const row = tbody.insertRow();

                    // --- Add Logging ---
                    // Log the raw item data to see what's coming in
                    console.log(`[Item ${index}] Raw Data:`, JSON.stringify(item));
                    // --- End Logging ---

                    // 1. PreceedingText+sup
                    row.insertCell().textContent = item?.fullContext || '';

                    // Extract text first
                    const displaySupText = extractVisibleSuperscriptText(item?.superscriptText || '');

                    // --- Add Logging ---
                    // Log the extracted text
                    console.log(`[Item ${index}] Extracted Text: "${displaySupText}"`);
                    // --- End Logging ---

                    // Determine if the tag exists but the extracted text is empty
                    const isTagPresentButEmpty = (item?.SupMissing !== true) && (displaySupText === '');

                    // --- Add Logging ---
                    // Log the values used in the condition and the result
                    console.log(`[Item ${index}] SupMissing: ${item?.SupMissing}, displaySupText === '': ${displaySupText === ''}, Result (isTagPresentButEmpty): ${isTagPresentButEmpty}`);
                    // --- End Logging ---

                    // 2. SuperscriptText Column
                    const supTextCell = row.insertCell();
                    supTextCell.textContent = isTagPresentButEmpty ? 'BLANK' : displaySupText;

                    // 3. SupMissing? Column
                    const supMissingCell = row.insertCell();
                    supMissingCell.textContent = isTagPresentButEmpty ? 'YES' : (item?.SupMissing === true ? 'true' : (item?.SupMissing === false ? 'false' : 'N/A'));

                    // 4. SupPosition Column
                    const posCell = row.insertCell();
                    if (isTagPresentButEmpty) {
                        posCell.textContent = 'BLANK';
                        posCell.className = 'position-cell pos-blank';
                    } else {
                        const position = item?.position;
                        posCell.className = `position-cell ${getPositionClass(position)}`;
                        posCell.innerHTML = getPositionIcon(position) + (position || 'N/A');
                    }

                } catch (itemError) {
                    console.error(`[Comparer Window] ERROR processing item at index ${index} in createResultsTable for ${urlForContext}:`, itemError, "Item data:", item);
                    const itemErrorRow = tbody.insertRow();
                    const cell = itemErrorRow.insertCell();
                    cell.colSpan = headers.length;
                    cell.textContent = `Error processing item ${index + 1}. Check console for details.`;
                    cell.style.color = 'orange';
                }
            });
        } catch (tableBuildError) {
            console.error(`[Comparer Window] CRITICAL ERROR building results table for ${urlForContext}:`, tableBuildError);
            try {
                 const tbody = table.querySelector('tbody') || table.createTBody();
                 const criticalErrorRow = tbody.insertRow();
                 const cell = criticalErrorRow.insertCell();
                 cell.colSpan = headers.length;
                 cell.textContent = "Critical error building table. Check console.";
                 cell.style.color = 'red'; cell.style.fontWeight = 'bold';
            } catch (fallbackError) { /* Ignore */ }
        }
        return table;
    }
    // --- Initial Load ---
    console.log("[Results Summary] Calling loadAndDisplayResults...");
    loadAndDisplayResults();

// (Confirmed working version ~625 lines with Auto-Save integrated - Attempt 3)
});// --- results_summary.js ---
document.addEventListener('DOMContentLoaded', () => {
    console.log("[Results Summary] DOMContentLoaded fired.");

    const resultsContainer = document.getElementById('resultsContainer');
	
    const summaryDiv = document.getElementById('summary');
    // --- Button reference and listener removed for auto-save ---

    const CURRENT_RESULTS_KEY = "multiPageScanResults_Current";
    const PREVIOUS_RESULTS_KEY = "multiPageScanResults_Previous";
    const STATUS_STORAGE_KEY = "multiPageScanStatus";

    // --- Helper Function for Icons ---
    function getPositionIcon(position) {
        // Keep this simple, handle potential undefined input gracefully
        switch(position){
            case 'ABOVE BASELINE': return '<span class="pos-icon pos-above" title="Above Baseline">✅</span>';
            case 'SAMELINE': return '<span class="pos-icon pos-sameline" title="Same Line">❌</span>';
            case 'BLANK': return '<span class="pos-icon pos-blank" title="Blank/Empty">⚠️</span>';
            case 'CHECK_ERROR': return '<span class="pos-icon pos-error" title="Position Check Error">❗</span>';
            default: return '<span class="pos-icon pos-unknown" title="Unknown/Invalid">❓</span>'; // Handle null/undefined/other
        }
    }

    // --- Helper Function for CSS Class ---
    function getPositionClass(position) {
        // Keep this simple, handle potential undefined input gracefully
        switch(position){
            case 'ABOVE BASELINE': return 'pos-above';
            case 'SAMELINE': return 'pos-sameline';
            case 'BLANK': return 'pos-blank';
            case 'CHECK_ERROR': return 'pos-error';
            default: return 'pos-unknown'; // Handle null/undefined/other
        }
    }

    // --- Helper Function to check if text is purely numeric ---
    function isNumeric(str) {
        if (typeof str !== 'string' || !str.trim()) return false; // Check if string and not empty/whitespace
        return /^\d+$/.test(str.trim()); // Check if it contains only digits
    }

    /**
     * Extracts the meaningful visible text from a superscript's outerHTML string OR plain text.
     * Prioritizes finding text nodes directly within the sup or its immediate children,
     * after attempting to ignore hidden elements via innerText.
     * Includes logging for diagnostics.
     *
     * @param {string} inputString The outerHTML string of the <sup> element OR potentially just plain text.
     * @returns {string} The extracted visible text, or an empty string if none found.
     */
    function extractVisibleSuperscriptText(inputString) {
      if (!inputString || typeof inputString !== 'string') {
        return '';
      }
      const trimmedInput = inputString.trim();
      // console.log(`[extractText] Raw Input: ${trimmedInput}`); // Log raw input

      // Handle cases where input is likely plain text already
      if (!trimmedInput.includes('<') && !trimmedInput.includes('>')) {
          // console.log(`[extractText] Input "${trimmedInput}" treated as plain text.`);
          if (trimmedInput.includes('Opens a modal dialog')) {
              const match = trimmedInput.match(/Opens a modal dialog for footnote\s*(\d+)/);
              if (match && match[1]) return match[1];
              return '';
          }
          return trimmedInput;
      }

      // Proceed with parsing only if it looks like HTML
      try {
        const parser = new DOMParser();
        const doc = parser.parseFromString(inputString, 'text/html');
        const supElement = doc.body.firstChild;

        if (!supElement || supElement.nodeName !== 'SUP') {
          // console.warn("[extractText] Could not parse sup element from string or not a SUP:", inputString); // Reduce noise
          const simpleMatch = inputString.match(/<sup>(.*?)<\/sup>/i);
          return simpleMatch ? simpleMatch[1].trim() : '';
        }

        let extractedText = '';
        // Strategy 1: innerText
        extractedText = supElement.innerText?.trim();
        // Strategy 2: Manual Node Traversal
        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
            let potentialText = '';
            const childNodes = Array.from(supElement.childNodes);
            for (const node of childNodes) {
                 if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) { potentialText = node.textContent.trim(); break; }
                 else if (node.nodeType === Node.ELEMENT_NODE) {
                     const grandChildNodes = Array.from(node.childNodes);
                     for (const grandNode of grandChildNodes) { if (grandNode.nodeType === Node.TEXT_NODE && grandNode.textContent?.trim()) { potentialText = grandNode.textContent.trim(); } }
                     if (potentialText) break;
                 }
            }
            if (potentialText) extractedText = potentialText;
        }
        // Strategy 3: Fallback with Node Removal
        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
            const clone = supElement.cloneNode(true);
            clone.querySelectorAll('.hidden, .sr-only, .visually-hidden, [aria-hidden="true"], [data-translation-text]').forEach(hiddenEl => hiddenEl.remove());
            let textAfterRemoval = clone.textContent?.trim() || '';
             if (textAfterRemoval) extractedText = textAfterRemoval;
        }
        // console.log(`[extractText] Input: ${inputString}, Final Output: "${extractedText}"`); // Reduce noise
        return extractedText || '';
      } catch (e) {
        console.error("[extractText] Error parsing outerHTML:", e, "Input:", inputString);
        return '';
      }
    }

      // --- Function to trigger saving the current page (with embedded collapse script) ---
        // --- Function to trigger saving the current page (with embedded collapse script) ---
    function saveCurrentPage() {
        console.log("Attempting to auto-save page...");
        try {
            // --- Create the JavaScript snippet to embed ---
            const embedScriptContent = `
document.addEventListener('DOMContentLoaded', () => {
    console.log("Saved page script running to attach collapse listeners.");
    const headers = document.querySelectorAll('.page-results-card .card-header');
    if (headers.length > 0) {
        console.log(\`Found \${headers.length} card headers to attach listeners.\`);
        headers.forEach(header => {
            // Check if listener already attached
            if (!header.dataset.collapseListenerAttached) {
                header.addEventListener('click', () => {
                    header.classList.toggle('collapsed');
                });
                header.dataset.collapseListenerAttached = 'true'; // Mark as attached
            }
        });
    } else {
        console.log("No card headers found on saved page to attach listeners.");
    }
});
`;
            // --- End snippet creation ---

            // 1. Clone the current document element
            const currentDocClone = document.documentElement.cloneNode(true);

            // 2. Find the body tag in the clone
            const bodyClone = currentDocClone.querySelector('body');

            if (bodyClone) {
                // 3. Create a new script element for the embed code
                const scriptElement = document.createElement('script');
                scriptElement.textContent = embedScriptContent;

                // 4. Append the new script element to the end of the cloned body
                bodyClone.appendChild(scriptElement);

                // 5. Remove the original script tag reference from the clone <<--- THIS IS THE KEY PART
                const originalScriptTag = bodyClone.querySelector('script[src="results_summary.js"]');
                if (originalScriptTag) {
                    originalScriptTag.remove(); // <-- Make sure this line executes
                    console.log("Removed original script tag from saved HTML clone.");
                } else {
                    // Log if the script tag wasn't found, which might indicate an issue elsewhere
                    console.warn("Original script tag 'script[src=\"results_summary.js\"]' not found in clone to remove.");
                }
                // --- End Key Part ---

            } else {
                console.warn("Could not find body element in cloned document to inject script.");
            }

            // 6. Get the modified HTML content from the clone
            const currentHtmlWithScript = '<!DOCTYPE html>\n' + currentDocClone.outerHTML; // Add Doctype back

            // 7. Create a Blob
            const blob = new Blob([currentHtmlWithScript], { type: 'text/html;charset=utf-8' });

            // 8. Create a temporary link
            const link = document.createElement('a');

            // 9. Generate filename with timestamp
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const filename = `results_summary_${timestamp}.html`;

            // 10. Set link attributes
            link.href = URL.createObjectURL(blob);
            link.download = filename;
            link.style.display = 'none';

            // 11. Append, click, and remove link
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            // 12. Revoke the Object URL
            URL.revokeObjectURL(link.href);

            console.log(`Auto-save dialog triggered for ${filename} (with embedded collapse script)`);

        } catch (error) {
            console.error("Error auto-saving page:", error);
        }
    }
    // --- End save function ---


    // --- Main Function to Load and Display ---
    async function loadAndDisplayResults() {
        console.log("[Results Summary] loadAndDisplayResults started.");
        let resultsSuccessfullyLoaded = false; // Flag to track success

        // Ensure summaryDiv and resultsContainer are valid at the start
        if (!summaryDiv || !resultsContainer) {
            console.error("[Results Summary] CRITICAL: Summary or Results container element not found!");
            if (resultsContainer) resultsContainer.innerHTML = '<div class="error-message">Page structure error: Core elements missing.</div>';
            else if (summaryDiv) summaryDiv.innerHTML = '<div class="error-message">Page structure error: Core elements missing.</div>';
            else document.body.innerHTML = '<div class="error-message">Page structure error: Core elements missing.</div>';
            return; // Stop execution
        }

        resultsContainer.innerHTML = '<div class="loading">Loading results...</div>';
        summaryDiv.innerHTML = 'Loading summary...';

        try {
            console.log("[Results Summary] Attempting to get data from storage...");
            const storedData = await chrome.storage.local.get([
                CURRENT_RESULTS_KEY,
                PREVIOUS_RESULTS_KEY,
                STATUS_STORAGE_KEY
            ]);
            console.log("[Results Summary] Data fetched from storage:", storedData);

            const currentResults = storedData[CURRENT_RESULTS_KEY];
            const previousResults = storedData[PREVIOUS_RESULTS_KEY];
            const statusInfo = storedData[STATUS_STORAGE_KEY];

            if (!currentResults || Object.keys(currentResults).length === 0) {
                console.log("[Results Summary] No current results found in storage.");
                resultsContainer.innerHTML = '<div class="no-results">No current scan results found. Run a scan or check background logs.</div>';
                summaryDiv.textContent = 'No current scan results available.';
                // Do NOT auto-save if no results
                return; // Exit if no results
            }

            console.log("[Results Summary] Current results found. Proceeding to display.");
            resultsContainer.innerHTML = ''; // Clear loading message

            // --- Calculate Summary Metrics ---
            let totalUrls = Object.keys(currentResults).length;
            let successCount = 0, errorCount = 0, totalItemsFound = 0, urlsWithIssues = 0;
            Object.values(currentResults).forEach(result => {
                if (!result) return;
                if (result.status === 'success') {
                    successCount++;
                    if (result.data && Array.isArray(result.data) && result.data.length > 0) {
                        totalItemsFound += result.data.length;
                        if (result.data.some(item => item && (item.position === 'SAMELINE' || item.position === 'BLANK' || item.position === 'CHECK_ERROR'))) {
                            urlsWithIssues++;
                        }
                    }
                } else {
                    errorCount++;
                }
            });
            console.log("[Results Summary] Summary calculated:", { totalUrls, successCount, errorCount, totalItemsFound, urlsWithIssues });

            // --- Display Enhanced Summary ---
            try {
                 const completedTimeStr = statusInfo?.completedTime ? `<span class="scan-time">Scan completed on: ${new Date(statusInfo.completedTime).toLocaleString()}</span>` : '';
                 const runningStatusStr = statusInfo?.running ? `<span class="scan-time">Scan is currently running... Refresh for updates.</span>` : '';
                 summaryDiv.innerHTML = `
                    <span class="metric metric-total"><strong>${totalUrls}</strong> URLs Scanned</span> |
                    <span class="metric metric-success"><strong>${successCount}</strong> Succeeded</span> |
                    <span class="metric metric-error"><strong>${errorCount}</strong> Failed</span> |
                    <span class="metric metric-total"><strong>${totalItemsFound}</strong> Items Found</span> |
                    <span class="metric ${urlsWithIssues > 0 ? 'metric-error' : 'metric-success'}"><strong>${urlsWithIssues}</strong> URLs with Issues</span>
                    ${completedTimeStr}
                    ${runningStatusStr}
                `;
                console.log("[Results Summary] summaryDiv.innerHTML updated successfully.");
            } catch (summaryUpdateError) {
                 console.error("[Results Summary] ERROR occurred while updating summaryDiv.innerHTML:", summaryUpdateError);
                 summaryDiv.textContent = 'Error displaying summary. Check console.';
            }
             console.log("[Results Summary] Summary display attempt finished.");

            // --- Display Results Per URL using Cards ---
            const sortedUrls = Object.keys(currentResults).sort();
            console.log(`[Results Summary] Processing ${sortedUrls.length} URLs.`);

            for (const url of sortedUrls) {
                const currentResultItem = currentResults[url];
                if (!currentResultItem) { console.warn(`[Results Summary] Skipping invalid result entry for URL key: ${url}`); continue; }
                const previousResultItem = previousResults ? previousResults[url] : null;

                const cardDiv = document.createElement('div');
                cardDiv.className = 'page-results-card';

                // --- Card Header ---
                const cardHeader = document.createElement('div');
                const hasIssues = currentResultItem.data && Array.isArray(currentResultItem.data) && currentResultItem.data.some(item => item && (item.position === 'SAMELINE' || item.position === 'BLANK' || item.position === 'CHECK_ERROR'));
                const startCollapsed = currentResultItem.status === 'error' || !hasIssues;
                cardHeader.className = `card-header ${startCollapsed ? 'collapsed' : ''}`;

                // --- Header Content Wrapper ---
                const headerContent = document.createElement('div');
                headerContent.className = 'header-content';

                // --- Header Main Line ---
                const headerMainLine = document.createElement('div');
                headerMainLine.className = 'header-main-line';
                const urlHeading = document.createElement('div');
                urlHeading.className = 'page-url';
                urlHeading.textContent = url;
                const statusBadge = document.createElement('span');
                statusBadge.className = 'status-badge';
                headerMainLine.appendChild(urlHeading);
                headerMainLine.appendChild(statusBadge);

                // --- Comparison Info Line ---
                const comparisonDiv = document.createElement('div');
                comparisonDiv.className = 'comparison-info';
                let comparisonText = 'SupCount(Prev.Rel/Sprint): N/A';
                let currentCount = 0, currentCountDisplay = '<strong>0</strong>', detailCountsString = '';

                if (currentResultItem.status === 'success' && currentResultItem.data && Array.isArray(currentResultItem.data)) {
                    currentCount = currentResultItem.data.length;
                    currentCountDisplay = `<strong>${currentCount}</strong>`;

                    // Calculate Detail Counts
                    let posAbove = 0, posSame = 0, posBlank = 0, posOther = 0;
                    let missingTrue = 0, missingFalse = 0;
                    let textNumeric = 0, textNonNumeric = 0, textBlank = 0;

                    currentResultItem.data.forEach(item => {
                        if (!item) return;
                        switch(item.position) { case 'ABOVE BASELINE': posAbove++; break; case 'SAMELINE': posSame++; break; case 'BLANK': posBlank++; break; default: posOther++; break; }
                        // --- MODIFICATION: Use SupMissing ---
                        if (item.SupMissing === true) { // Use SupMissing as requested
                            missingTrue++;
                        } else {
                            missingFalse++;
                        }
                        // --- END MODIFICATION ---
                        const extractedText = extractVisibleSuperscriptText(item.superscriptText);
                        if (!extractedText) textBlank++;
                        else if (isNumeric(extractedText)) textNumeric++;
                        else textNonNumeric++;
                    });

                    detailCountsString = `[` + `Pos: ${posAbove}(✓) ${posSame}(✗) ${posBlank}(⚠️) ${posOther}(?) | ` + `Missing: ${missingTrue}(T) ${missingFalse}(F) | ` + `Text: ${textNumeric}(#) ${textNonNumeric}(Ab) ${textBlank}(Bl)` + `]`;

                } else if (currentResultItem.status === 'error') { currentCountDisplay = '<strong>Error</strong>'; currentCount = NaN; }
                else if (currentResultItem.status !== 'success') { currentCountDisplay = '<strong>N/A</strong>'; currentCount = NaN; }

                const currentHeaderText = `SupCount(Current.Rel/Sprint): ${currentCountDisplay}`;

                // --- Comparison Logic (Full version) ---
                if (previousResultItem) {
                    // Add safety check for previousResultItem.data being an array
                    if (previousResultItem.status === 'success' && previousResultItem.data && Array.isArray(previousResultItem.data)) {
                        const previousCount = previousResultItem.data.length;
                        // --- User Header Change ---
                        comparisonText = `SupCount(Prev.Rel/Sprint): <strong>${previousCount}</strong> | ${currentHeaderText}`;
                        // --- End User Header Change ---

                        // Only add indicators and PASS/FAIL if both counts are valid numbers
                        if (!isNaN(currentCount) && typeof previousCount === 'number') {
                            const change = currentCount - previousCount;
                            let changeClass = 'change-same';
                            let changeSymbol = '●'; // Default to same
                            let passFailStatus = ''; // Initialize Pass/Fail status
                            let passFailClass = '';
                            let passFailDisplay = ''; // Display string (may include strong tag)
                            let extraSpacing = ''; // Variable for extra spaces

                            if (change > 0) {
                                changeClass = 'change-increase';
                                changeSymbol = '▲';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else if (change < 0) {
                                changeClass = 'change-decrease';
                                changeSymbol = '▼';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else { // change === 0
                                changeClass = 'change-same';
                                changeSymbol = '●';
                                passFailStatus = 'PASS';
                                passFailClass = 'status-pass';
                                passFailDisplay = `<strong>${passFailStatus}</strong>`;
                                // --- ADD SPACING FOR PASS ---
                                extraSpacing = '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'; // 8 non-breaking spaces
                                // --- END ADD SPACING ---
                            }

                            // Append change indicator and PASS/FAIL status
                            comparisonText += `<span class="change-indicator ${changeClass}">${changeSymbol} ${change !== 0 ? change : ''}</span>`;
                            comparisonText += ` <span class="pass-fail-status ${passFailClass}">${passFailDisplay}</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---

                        } else {
                             // Handle cases where one count is 'Error' or invalid
                             comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                             // --- Append Detail Counts String WRAPPED in a span ---
                             comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                             // --- End Append ---
                        }

                    } else if (previousResultItem.status === 'error') {
                         // --- User Header Change ---
                         comparisonText = `SupCount(Prev.Rel/Sprint): <strong>Error</strong> | ${currentHeaderText}`;
                         // --- End User Header Change ---
                         comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                         // --- Append Detail Counts String WRAPPED in a span ---
                         comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                         // --- End Append ---
                    } else { // Previous existed but wasn't success/error...
                         const previousCount = 0; // Assume 0 for comparison
                         // --- User Header Change ---
                         comparisonText = `SupCount(Prev.Rel/Sprint): <strong>0</strong> | ${currentHeaderText}`;
                         // --- End User Header Change ---
                         if (!isNaN(currentCount)) { // Compare if current is a number
                            const change = currentCount - previousCount;
                            let changeClass = 'change-same';
                            let changeSymbol = '●';
                            let passFailStatus = '';
                            let passFailClass = '';
                            let passFailDisplay = '';
                            let extraSpacing = ''; // Variable for extra spaces

                            if (change > 0) {
                                // Ensure all assignments are properly separated by semicolons
                                changeClass = 'change-increase';
                                changeSymbol = '▲';
                                passFailStatus = 'FAIL';
                                passFailClass = 'status-fail';
                                passFailDisplay = passFailStatus;
                            } else { // change === 0 (since previousCount is 0)
                                changeClass = 'change-same';
                                changeSymbol = '●';
                                passFailStatus = 'PASS';
                                passFailClass = 'status-pass';
                                passFailDisplay = `<strong>${passFailStatus}</strong>`;
                                // --- ADD SPACING FOR PASS ---
                                extraSpacing = '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'; // 8 non-breaking spaces
                                // --- END ADD SPACING ---
                            }
                            comparisonText += `<span class="change-indicator ${changeClass}">${changeSymbol} ${change !== 0 ? change : ''}</span>`;
                            comparisonText += ` <span class="pass-fail-status ${passFailClass}">${passFailDisplay}</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---
                         } else {
                            comparisonText += ` <span class="pass-fail-status status-na">N/A</span>`;
                            // --- Append Detail Counts String WRAPPED in a span ---
                            comparisonText += `<span class="detail-counts">${detailCountsString}</span>`;
                            // --- End Append ---
                         }
                    }
                } else { // No previous result item
                     // --- User Header Change ---
                     comparisonText = `SupCount(Prev.Rel/Sprint): N/A | ${currentHeaderText}`;
                     // --- End User Header Change ---
                     // --- Append Detail Counts String WRAPPED in a span ---
                     // Ensure detailCountsString is appended even if it's empty (will just be '[]')
                     comparisonText += `<span class="detail-counts">${detailCountsString || '[]'}</span>`;
                     // --- End Append ---
                }
                comparisonDiv.innerHTML = comparisonText;
                // --- End Comparison Logic ---


                // Assemble Header Content
                headerContent.appendChild(headerMainLine);
                headerContent.appendChild(comparisonDiv);

                // Toggle Icon
                const toggleIcon = document.createElement('span');
                toggleIcon.className = 'toggle-icon';
                toggleIcon.innerHTML = '&#9660;';

                // Add Content and Icon to Header
                cardHeader.appendChild(headerContent);
                cardHeader.appendChild(toggleIcon);

                // --- Card Body ---
                const cardBody = document.createElement('div');
                cardBody.className = 'card-body';

                // Populate Card Body and Status Badge
                if (currentResultItem.status === 'error') {
                    statusBadge.classList.add('error'); statusBadge.textContent = 'Error';
                    const errorP = document.createElement('div'); errorP.className = 'error-message'; errorP.textContent = `Error: ${currentResultItem.error || 'Unknown error'}`; cardBody.appendChild(errorP);
                } else if (currentResultItem.status === 'success' && currentResultItem.data && Array.isArray(currentResultItem.data)) {
                    const data = currentResultItem.data;
                    if (data.length === 0) { statusBadge.classList.add('no-items'); statusBadge.textContent = 'No Items Found'; cardBody.innerHTML = '<div class="no-results">Scan successful, no superscripts found on this page.</div>'; }
                    else {
                        statusBadge.classList.add('success'); statusBadge.textContent = `Found ${data.length}`;
                        const tableElement = createResultsTable(data, url); // Pass URL for logging context
                        if (tableElement instanceof Node) { cardBody.appendChild(tableElement); }
                        else { console.error(`[Results Summary] FALLBACK ERROR: createResultsTable did not return a Node for URL "${url}". Returned:`, tableElement); const errorPlaceholder = document.createElement('div'); errorPlaceholder.className = 'error-message'; errorPlaceholder.textContent = 'Internal error: Failed to display results table (Fallback). Check console.'; cardBody.appendChild(errorPlaceholder); }
                    }
                } else { // Handle cases where status isn't success or data is missing/not array
                    statusBadge.classList.add('error'); statusBadge.textContent = 'Unknown/Invalid Status';
                    const unknownP = document.createElement('div'); unknownP.className = 'error-message'; unknownP.textContent = `Unknown status ('${currentResultItem.status}') or missing/invalid data.`; cardBody.appendChild(unknownP);
                }

                // Assemble Card
                cardDiv.appendChild(cardHeader);
                cardDiv.appendChild(cardBody);
                resultsContainer.appendChild(cardDiv);

                // Add Collapse Toggle Listener
                cardHeader.addEventListener('click', () => { cardHeader.classList.toggle('collapsed'); });
            }
            console.log("[Results Summary] Finished processing URLs.");
            resultsSuccessfullyLoaded = true; // Mark as successful

        } catch (error) {
            console.error("[Results Summary] Caught error in loadAndDisplayResults (main try block):", error);
            if (summaryDiv) summaryDiv.textContent = 'Error loading results.';
            if (resultsContainer) resultsContainer.innerHTML = `<div class="error-message">Error loading results: ${error.message}. Check console.</div>`;
            // Do NOT auto-save on error
        }

        console.log("[Results Summary] loadAndDisplayResults finished.");

        // --- Trigger Auto-Save ---
        if (resultsSuccessfullyLoaded) {
            // Add a small delay to ensure the DOM is fully updated visually
            setTimeout(saveCurrentPage, 500); // Delay 500ms before saving
        }
        // --- End Auto-Save Trigger ---
    }

    // --- Function to Create the Results Table ---
    /**
 * Creates an HTML table element to display the superscript comparison results.
 * Handles the special case where a sup tag exists but is empty.
 *
 * @param {Array<Object>} data - Array of result items from the scan.
 * @param {string} [urlForContext='Unknown URL'] - The URL the data pertains to, for logging.
 * @returns {HTMLTableElement} The generated table element.
 */
function createResultsTable(data, urlForContext = 'Unknown URL') {
    const table = document.createElement('table');
    try {
        const thead = table.createTHead();
        const tbody = table.createTBody();
        const headerRow = thead.insertRow();
        // Headers remain the same
        const headers = ['PreceedingText+sup', 'SuperscriptText', 'SupMissing?', 'SupPosition'];
        headers.forEach(text => {
            const th = document.createElement('th');
            th.textContent = text;
            headerRow.appendChild(th);
        });

        // Input validation
        if (!Array.isArray(data)) {
            console.error(`[Results Summary] ERROR in createResultsTable for ${urlForContext}: Input data is not an array.`, data);
            const errorRow = tbody.insertRow();
            const cell = errorRow.insertCell();
            cell.colSpan = headers.length;
            cell.textContent = "Error: Invalid data format received.";
            cell.style.color = 'red';
            cell.style.fontStyle = 'italic';
            return table;
        }

        // Process each result item
        data.forEach((item, index) => {
            try {
                const row = tbody.insertRow();

                // 1. PreceedingText+sup (Existing logic - unchanged)
                row.insertCell().textContent = item?.fullContext || '';

                // --- Start Integration of Empty Tag Logic ---

                // Extract visible text using the helper function
                const rawSupText = item?.superscriptText || ''; // Get raw text/HTML from item
                const displaySupText = extractVisibleSuperscriptText(rawSupText);

                // Determine if the tag exists but the extracted text is empty
                // A tag exists if SupMissing is explicitly false, or if it's null/undefined (not explicitly true)
                const isTagPresentButEmpty = (item?.SupMissing !== true) && (displaySupText === '');

                // --- Diagnostic Logging (Optional but helpful) ---
                // console.log(`[Results Summary] Item ${index} (${urlForContext}): SupMissing=${item?.SupMissing}, ExtractedText="${displaySupText}", isTagPresentButEmpty=${isTagPresentButEmpty}`);
                // ---

                // 2. SuperscriptText Column
                const supTextCell = row.insertCell();
                if (isTagPresentButEmpty) {
                    supTextCell.textContent = 'BLANK';
                    supTextCell.classList.add('pos-blank'); // Apply styling for blank
                } else {
                    supTextCell.textContent = displaySupText; // Original logic
                }

                // 3. SupMissing? Column
                const supMissingCell = row.insertCell();
                if (isTagPresentButEmpty) {
                    supMissingCell.textContent = 'YES'; // Indicate empty tag found
                    // Optional: Add specific styling for this "YES"
                    supMissingCell.style.fontWeight = 'bold';
                    supMissingCell.style.color = '#e74c3c'; // Example: Use same color as 'sameline' or 'blank'
                } else {
                    // Original logic: Display true/false/N/A based on item.SupMissing
                    if (item?.SupMissing === true) {
                        supMissingCell.textContent = 'true';
                    } else if (item?.SupMissing === false) {
                        supMissingCell.textContent = 'false';

                    } else {
                        supMissingCell.textContent = 'N/A'; // Handle null/undefined cases
                    }
                }

                // 4. SupPosition Column
                const posCell = row.insertCell();
                if (isTagPresentButEmpty) {
                    posCell.textContent = 'BLANK';
                    posCell.className = 'position-cell pos-blank'; // Apply blank styling	
                } else {
                    // Original logic for position when tag has text or is missing
                    const position = item?.position;
                    // Ensure helper functions handle potential values like 'CHECK_ERROR'
                    posCell.className = `position-cell ${getPositionClass(position)}`;
                    posCell.innerHTML = getPositionIcon(position) + (position || 'N/A');
                }

                // --- End Integration ---

            } catch (itemError) {
                // Existing item-level error handling (unchanged)
                console.error(`[Results Summary] ERROR processing item at index ${index} in createResultsTable for ${urlForContext}:`, itemError, "Item data:", item);
                const itemErrorRow = tbody.insertRow();
                const cell = itemErrorRow.insertCell();
                cell.colSpan = headers.length;
                cell.textContent = `Error processing item ${index + 1}. Check console for details.`;
                cell.style.color = 'orange';
            }
        });
    } catch (tableBuildError) {
        // Existing table-level error handling (unchanged)
        console.error(`[Results Summary] CRITICAL ERROR building results table for ${urlForContext}:`, tableBuildError);
        try {
            const tbody = table.querySelector('tbody') || table.createTBody();
            const criticalErrorRow = tbody.insertRow();
            const cell = criticalErrorRow.insertCell();
            cell.colSpan = headers.length;
            cell.textContent = "Critical error building table. Check console.";
            cell.style.color = 'red';
            cell.style.fontWeight = 'bold';
        } catch (fallbackError) { /* Ignore */ }
    }
    return table;
}

    // --- Initial Load ---
    console.log("[Results Summary] Calling loadAndDisplayResults...");
    loadAndDisplayResults();

    // --- Button Listener Removed ---

});