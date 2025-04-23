// --- popup.js ---
// Handles the analysis of the CURRENT ACTIVE TAB when the popup is clicked.
document.addEventListener('DOMContentLoaded', () => {
    const resultsBody = document.getElementById('resultsBody');
    const footerStatusDiv = document.getElementById('footerStatus');
    const saveButton = document.getElementById('saveButton');
    const emailButton = document.getElementById('emailButton');
    const activeUrlDisplay = document.getElementById('activeUrlDisplay');
    // *** RENAMED: Get reference to the renamed blank count element ***
    const blankSupMissingCountDisplay = document.getElementById('blankSupMissingCountDisplay');
    // *** NEW: Get reference to the new top-right SupMissing count element ***
    const supMissingCountDisplay = document.getElementById('supMissingCountDisplay');

    const STORAGE_KEY = 'previousSuperscriptResults_ActiveTab';

    let resultsLoadedAtInit = [];
    let comparisonOutputForDisplay = [];

    // --- Helper Functions ---
    function updateFooterStatus(message, type = 'info') {
        if (footerStatusDiv) {
            footerStatusDiv.textContent = message;
            footerStatusDiv.className = 'footer-status'; // Reset classes
            if (type === 'error') footerStatusDiv.classList.add('error');
            else if (type === 'success') footerStatusDiv.classList.add('success');
        }
        console.log(`Popup Footer Status: ${message} (${type})`);
    }

    function updateUrlDisplay(url) {
        if (activeUrlDisplay) {
            if (url) {
                activeUrlDisplay.textContent = url;
                activeUrlDisplay.title = url;
            } else {
                activeUrlDisplay.textContent = 'Could not get URL';
                activeUrlDisplay.title = 'Could not get URL';
            }
        }
    }

    // *** RENAMED: Function to update Blank (Tag Present but Empty) Count display ***
    // *** UPDATED: Changed label text ***
    function updateBlankSupMissingCountDisplay(count) {
        if (blankSupMissingCountDisplay) {
            blankSupMissingCountDisplay.textContent = `SupMissing: ${count}`; // Changed Label
        }
    }

    // *** NEW: Function to update Top-Right SupMissing (Tag Not Found) Count display ***
    function updateSupMissingCountDisplay(count) {
        if (supMissingCountDisplay) {
            supMissingCountDisplay.textContent = `Missing: ${count}`;
        }
    }

    function getPositionClass(position) {
        switch(position){
            case 'ABOVE BASELINE': return 'pos-above';
            case 'SAMELINE': return 'pos-sameline';
            case 'BLANK': return 'pos-blank';
            case 'CHECK_ERROR': return 'pos-error';
            case 'UNKNOWN': return 'pos-unknown';
            default: return 'pos-unknown';
        }
    }

    function getStatusClass(status) {
        switch(status){
            case 'ADDED': return 'status-added';
            case 'REMOVED': return 'status-removed';
            case 'POSITION_MISMATCH': return 'status-mismatch';
            case 'MATCH':
            default: return 'status-match';
        }
    }

    function setActionButtonsState(enabled) {
        if(saveButton) saveButton.disabled = !enabled;
        if(emailButton) emailButton.disabled = !enabled;
        console.log(`Action buttons enabled: ${enabled}`);
    }

    // --- Robust Text Extraction Function ---
    // (Keep the existing extractVisibleSuperscriptText function as is)
    function extractVisibleSuperscriptText(inputString) {
        if (!inputString || typeof inputString !== 'string') {
            return '';
        }
        const trimmedInput = inputString.trim();

        if (!trimmedInput.includes('<') && !trimmedInput.includes('>')) {
            if (trimmedInput.includes('Opens a modal dialog')) {
                const match = trimmedInput.match(/Opens a modal dialog for footnote\s*(\d+)/);
                return (match && match[1]) ? match[1] : '';
            }
            return trimmedInput;
        }

        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(inputString, 'text/html');
            const supElement = doc.querySelector('sup');

            if (!supElement) {
                const simpleMatch = inputString.match(/<sup>(.*?)<\/sup>/i);
                let simpleText = simpleMatch ? simpleMatch[1].trim() : '';
                 if (simpleText.includes('Opens a modal dialog')) {
                     const match = simpleText.match(/Opens a modal dialog for footnote\s*(\d+)/);
                     simpleText = (match && match[1]) ? match[1] : '';
                 }
                return simpleText;
            }

            let extractedText = supElement.innerText?.trim();
            if (extractedText && extractedText.includes('Opens a modal dialog')) {
                 const match = extractedText.match(/Opens a modal dialog for footnote\s*(\d+)/);
                 extractedText = (match && match[1]) ? match[1] : extractedText.replace(/Opens a modal dialog for footnote\s*\d+/, '').trim();
            }

            if (!extractedText || extractedText.includes('Opens a modal dialog')) {
                let potentialText = '';
                const childNodes = Array.from(supElement.childNodes);
                for (const node of childNodes) {
                    if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) {
                        potentialText = node.textContent.trim();
                        break;
                    }
                }
                if (!potentialText) {
                     for (const node of childNodes) {
                         if (node.nodeType === Node.ELEMENT_NODE) {
                             const grandChildNodes = Array.from(node.childNodes);
                             for (const grandNode of grandChildNodes) {
                                 if (grandNode.nodeType === Node.TEXT_NODE && grandNode.textContent?.trim()) {
                                     potentialText = grandNode.textContent.trim();
                                     break;
                                 }
                             }
                         }
                         if (potentialText) break;
                     }
                }

                 if (potentialText && potentialText.includes('Opens a modal dialog')) {
                     const match = potentialText.match(/Opens a modal dialog for footnote\s*(\d+)/);
                     potentialText = (match && match[1]) ? match[1] : potentialText.replace(/Opens a modal dialog for footnote\s*\d+/, '').trim();
                 }

                if (potentialText && potentialText !== extractedText) {
                    extractedText = potentialText;
                }
            }

            if (!extractedText || extractedText.includes('Opens a modal dialog')) {
                const clone = supElement.cloneNode(true);
                clone.querySelectorAll('.hidden, .sr-only, .visually-hidden, [aria-hidden="true"], span[data-translation-text]').forEach(hiddenEl => {
                    hiddenEl.remove();
                });
                let textAfterRemoval = clone.textContent?.trim() || '';
                 if (textAfterRemoval && textAfterRemoval.includes('Opens a modal dialog')) {
                     const match = textAfterRemoval.match(/Opens a modal dialog for footnote\s*(\d+)/);
                     textAfterRemoval = (match && match[1]) ? match[1] : textAfterRemoval.replace(/Opens a modal dialog for footnote\s*\d+/, '').trim();
                 }
                if (textAfterRemoval) {
                    extractedText = textAfterRemoval;
                }
            }

            if (extractedText && extractedText.includes('Opens a modal dialog')) {
                 extractedText = extractedText.replace(/Opens a modal dialog for footnote\s*\d+/, '').trim();
            }

            return extractedText || '';

        } catch (e) {
            console.error("[extractText - popup.js] Error parsing outerHTML:", e, "Input:", inputString);
            return '';
        }
    }
    // --- End Text Extraction Function ---


    // --- Comparison Logic ---
    // (Keep the existing compareResults function as is)
    function compareResults(prevResults = [], currResults = []) {
        console.log(`Comparing ${currResults.length} current items against ${prevResults.length} previous items.`);
        const comparisonOutput = [];
        const createComparisonKey = (item) => `${item?.fullContext || ''}::${item?.superscriptText || ''}`;

        const prevMap = new Map(prevResults.map(item => [createComparisonKey(item), item]));
        const currMap = new Map(currResults.map(item => [createComparisonKey(item), item]));
        let mismatchCount = 0;

        for (const [key, currentItem] of currMap.entries()) {
            const prevItem = prevMap.get(key);
            let status = 'MATCH';
            if (prevItem) {
                if (currentItem.position !== prevItem.position) {
                    status = 'POSITION_MISMATCH';
                    mismatchCount++;
                }
                prevMap.delete(key);
            } else {
                status = 'ADDED';
                mismatchCount++;
            }
            comparisonOutput.push({ ...currentItem, status });
        }

        for (const [key, prevItem] of prevMap.entries()) {
            comparisonOutput.push({ ...prevItem, status: 'REMOVED' });
            mismatchCount++;
        }

        comparisonOutput.sort((a, b) => (a.fullContext || '').localeCompare(b.fullContext || ''));

        console.log(`Comparison resulted in ${comparisonOutput.length} display items with ${mismatchCount} mismatches.`);
        return { comparisonOutput, mismatchCount };
    }

    // --- Display Results ---
    function displayComparisonResults(comparisonData, mismatchCount) {
        resultsBody.innerHTML = '';
        comparisonOutputForDisplay = comparisonData;
        // *** RENAMED: Counter for blank tags (tag present but empty) ***
        let blankSupMissingItemCount = 0;
        // *** NEW: Counter for missing tags (SupMissing === true) ***
        let supMissingItemCount = 0;

        if (!comparisonData || comparisonData.length === 0) {
            const tr = resultsBody.insertRow();
            const td = tr.insertCell();
            td.colSpan = 5;
            td.className = 'loading-placeholder'; // Use the style
            td.textContent = 'No superscripts found in current scan.';
            updateFooterStatus("Scan complete. No superscripts found.");
            setActionButtonsState(false);
            // *** UPDATED: Call renamed/new functions with 0 ***
            updateBlankSupMissingCountDisplay(0);
            updateSupMissingCountDisplay(0);
            return false;
        }

        if (mismatchCount === 0) {
            updateFooterStatus("Comparison complete: No mismatches found against previous results.", "success");
        } else {
            updateFooterStatus(`Comparison complete: ${mismatchCount} mismatch(es) found.`, "info");
        }
        setActionButtonsState(true);

        comparisonData.forEach((item, index) => {
            const tr = resultsBody.insertRow();
            tr.className = getStatusClass(item.status);
            tr.title = `Status: ${item.status}. Click to navigate.`;

            const rawSupText = item.superscriptText || '';
            const displaySupText = extractVisibleSuperscriptText(rawSupText);
            const isTagPresentButEmpty = (item.SupMissing !== true) && (displaySupText === '');
            // *** NEW: Check if the tag itself is missing ***
            const isTagMissing = item.SupMissing === true;

            // *** UPDATED: Increment correct counters ***
            if (isTagPresentButEmpty) {
                blankSupMissingItemCount++;
            }
            if (isTagMissing) {
                supMissingItemCount++;
            }
            // ***

            const tdStatus = tr.insertCell();
            const tdContext = tr.insertCell();
            const tdScript = tr.insertCell();
            const tdMissing = tr.insertCell();
            const tdPosition = tr.insertCell();

            tdStatus.textContent = item.status?.replace('_', ' ') || 'N/A';
            tdStatus.style.textAlign = 'center';

            tdContext.textContent = item.fullContext || '';

            if (isTagPresentButEmpty) {
                tdScript.textContent = 'BLANK';
                tdScript.classList.add('pos-blank');
            } else {
                tdScript.textContent = displaySupText;
            }

            // Updated logic for SupMissing? column
            if (isTagMissing) { // Check if tag itself is missing first
                tdMissing.textContent = 'YES';
                tdMissing.style.fontWeight = 'bold';
                tdMissing.style.color = '#c0392b'; // Use a distinct color for truly missing
            } else if (isTagPresentButEmpty) { // Check if tag is present but blank
                 tdMissing.textContent = 'NO'; // Tag is present
                 // Optionally add a different style here if needed to indicate blank vs non-blank
            }
             else { // Tag is present and not blank
                tdMissing.textContent = 'NO';
            }
            tdMissing.style.textAlign = 'center';

            // Updated logic for Position column
            if (isTagMissing) { // If tag is missing, position is irrelevant
                 tdPosition.textContent = 'N/A';
                 tdPosition.style.fontStyle = 'italic';
                 tdPosition.style.color = '#7f8c8d';
            } else if (isTagPresentButEmpty) {
                tdPosition.textContent = 'BLANK';
                tdPosition.className = 'pos-blank';
            } else {
                tdPosition.textContent = item.position || 'UNKNOWN';
                tdPosition.className = getPositionClass(item.position);
            }
            tdPosition.style.textAlign = 'center';

            if (item.status !== 'REMOVED' && item.elementId) {
                 tr.addEventListener('click', () => handleRowClick(item.elementId, index + 1));
                 tr.style.cursor = 'pointer';
            } else {
                tr.style.cursor = 'default';
            }
        });

        // *** UPDATED: Call renamed/new functions with calculated counts ***
        updateBlankSupMissingCountDisplay(blankSupMissingItemCount);
        updateSupMissingCountDisplay(supMissingItemCount);
        return true;
    }

    // --- Generate Results HTML/Text for Saving/Email ---
    // (Keep generateResultsHTML and generateResultsText functions as they were,
    // they already use the updated YES/NO logic for SupMissing)
    function generateResultsHTML(results) {
        let tableRowsHTML = results.map(item => {
            const statusClass = getStatusClass(item.status);
            const positionClass = getPositionClass(item.position);
            const escapeHtml = (unsafe) => unsafe?.toString().replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;") || '';

            const rawSupText = item.superscriptText || '';
            const displaySupText = extractVisibleSuperscriptText(rawSupText);
            const isTagPresentButEmpty = (item.SupMissing !== true) && (displaySupText === '');
            const isTagMissing = item.SupMissing === true; // Added check

            const scriptContent = isTagPresentButEmpty ? 'BLANK' : escapeHtml(displaySupText);
            // Updated logic for missingContent
            const missingContent = isTagMissing ? 'YES' : 'NO';
            // Updated logic for positionContent
            const positionContent = isTagMissing ? 'N/A' : (isTagPresentButEmpty ? 'BLANK' : escapeHtml(item.position));
            const effectivePositionClass = isTagMissing ? '' : (isTagPresentButEmpty ? 'pos-blank' : positionClass); // Don't apply position class if missing

            return `<tr class="${escapeHtml(statusClass)}">
                        <td style="text-align: center;">${escapeHtml(item.status?.replace('_', ' '))}</td>
                        <td>${escapeHtml(item.fullContext)}</td>
                        <td>${scriptContent}</td>
                        <td style="text-align: center;">${missingContent}</td>
                        <td class="${escapeHtml(effectivePositionClass)}" style="text-align: center;">${positionContent}</td>
                    </tr>`;
        }).join('');

        const styles = `
            body { font-family: sans-serif; margin: 20px; }
            table { border-collapse: collapse; width: 100%; table-layout: auto; border: 1px solid #ccc; }
            th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; word-wrap: break-word; }
            th { background-color: #f2f2f2; position: sticky; top: 0; z-index: 1;}
            .pos-above { color: green; }
            .pos-sameline { color: red; }
            .pos-blank { color: orange; font-style: italic; }
            .pos-error { color: red; font-weight: bold; }
            .pos-unknown { color: grey; }
            .status-mismatch { background-color: lightyellow; }
            .status-added { background-color: lightgreen; }
            .status-removed { background-color: lightcoral; text-decoration: line-through; }
        `;

        return `<!DOCTYPE html>
                <html>
                <head>
                    <title>Superscript Comparison Results</title>
                    <meta charset="UTF-8">
                    <style>${styles}</style>
                </head>
                <body>
                    <h1>Superscript Comparison Results (${new Date().toLocaleString()})</h1>
                    <table>
                        <thead>
                            <tr>
                                <th>Status</th>
                                <th>Preceding Text</th>
                                <th>Superscript Text</th>
                                <th>SupMissing?</th>
                                <th>Position</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${tableRowsHTML}
                        </tbody>
                    </table>
                </body>
                </html>`;
    }

    function generateResultsText(results) {
        let text = `Superscript Comparison Results (${new Date().toLocaleString()})\n=============================================================\n\n`;
        results.forEach(item => {
            const rawSupText = item.superscriptText || '';
            const displaySupText = extractVisibleSuperscriptText(rawSupText);
            const isTagPresentButEmpty = (item.SupMissing !== true) && (displaySupText === '');
            const isTagMissing = item.SupMissing === true; // Added check

            const scriptContent = isTagPresentButEmpty ? 'BLANK' : displaySupText;
            const missingContent = isTagMissing ? 'YES' : 'NO'; // Updated logic
            const positionContent = isTagMissing ? 'N/A' : (isTagPresentButEmpty ? 'BLANK' : (item.position || 'UNKNOWN')); // Updated logic

            text += `Status: ${item.status?.replace('_', ' ') || 'N/A'}\n`;
            text += `Context: ${item.fullContext || ''}\n`;
            text += `Script: ${scriptContent}\n`;
            text += `SupMissing?: ${missingContent}\n`;
            text += `Position: ${positionContent}\n`;
            text += `-------------------------------------------------------------\n`;
        });
        return text;
    }


    // --- Save/Email Results Functions ---
    // (Keep saveResultsToFile, triggerLinkDownload, and emailResults functions as they were)
    function saveResultsToFile() {
        if (!comparisonOutputForDisplay || comparisonOutputForDisplay.length === 0) {
            updateFooterStatus("No results to save.", "error");
            return;
        }
        try {
            const htmlContent = generateResultsHTML(comparisonOutputForDisplay);
            const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
            const url = URL.createObjectURL(blob);

            if (chrome.downloads) {
                const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
                const filename = `superscript_results_${timestamp}.html`;
                chrome.downloads.download({
                    url: url,
                    filename: filename,
                    saveAs: true
                }, (downloadId) => {
                    if (chrome.runtime.lastError) {
                        console.error("Download failed:", chrome.runtime.lastError);
                        updateFooterStatus("Error initiating download.", "error");
                        triggerLinkDownload(url, filename);
                    } else {
                        console.log("Download started with ID:", downloadId);
                        updateFooterStatus("Save dialog initiated.");
                        setTimeout(() => URL.revokeObjectURL(url), 1000);
                    }
                });
            } else {
                console.warn("chrome.downloads API not available, using link fallback.");
                const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
                const filename = `superscript_results_${timestamp}.html`;
                triggerLinkDownload(url, filename);
                updateFooterStatus("Save dialog initiated (fallback).");
            }

        } catch (error) {
            console.error("Error saving results:", error);
            updateFooterStatus("Error preparing results for saving.", "error");
        }
    }

    function triggerLinkDownload(url, filename) {
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }


    function emailResults() {
        if (!comparisonOutputForDisplay || comparisonOutputForDisplay.length === 0) {
            updateFooterStatus("No results to email.", "error");
            return;
        }
        try {
            const subject = "Superscript Comparison Results";
            const body = generateResultsText(comparisonOutputForDisplay);
            if (body.length > 1800) {
                updateFooterStatus("Results too long for direct email body. Consider saving to file first.", "error");
            }
            const mailtoUrl = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
            window.open(mailtoUrl, '_blank');
            updateFooterStatus("Email client should be opening...");
        } catch (error) {
            console.error("Error preparing email:", error);
            updateFooterStatus("Error preparing results for email.", "error");
        }
    }


    // --- Event Handlers ---
    // (Keep handleRowClick function as is)
    function handleRowClick(elementId, index) {
        updateFooterStatus(`Navigating to item ${index}...`);
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
            if (!tabs || tabs.length === 0 || !tabs[0]?.id) {
                updateFooterStatus("Error: Couldn't find active tab for navigation.", "error");
                return;
            }
            chrome.tabs.sendMessage(
                tabs[0].id,
                { type: "SCROLL_TO_SUPERSCRIPT", elementId: elementId },
                (response) => {
                    if (chrome.runtime.lastError) {
                        updateFooterStatus(`Error navigating: ${chrome.runtime.lastError.message}`, "error");
                    } else if (response?.success) {
                        updateFooterStatus(`Navigated to item ${index}.`);
                    } else {
                        updateFooterStatus(`Could not find item ${index} on the page (or navigation failed).`);
                    }
                }
            );
        });
    }

    // --- Initialization Logic (Loads/Requests for Active Tab) ---
    async function initialize() {
        updateFooterStatus("Loading previous results & requesting scan for active tab...");
        resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">Loading previous results & requesting scan for active tab...</td></tr>`;
        setActionButtonsState(false);
        updateUrlDisplay('Loading URL...');
        // *** UPDATED: Call renamed/new functions with initial state ***
        updateBlankSupMissingCountDisplay('-');
        updateSupMissingCountDisplay('-');

        try {
            console.log("Popup (Active Tab): Loading results from storage...");
            const result = await chrome.storage.local.get([STORAGE_KEY]);
            resultsLoadedAtInit = result[STORAGE_KEY] || [];
            console.log(`Popup (Active Tab): Loaded ${resultsLoadedAtInit.length} previous results.`);

            console.log("Popup (Active Tab): Requesting scan from content script.");
            const tabs = await chrome.tabs.query({ active: true, currentWindow: true });

            const currentUrl = tabs?.[0]?.url;
            updateUrlDisplay(currentUrl);

            if (!tabs || tabs.length === 0 || !tabs[0]?.id) {
                updateFooterStatus("Error: Couldn't find the active tab.", "error");
                setActionButtonsState(false);
                resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">Error: Couldn't find the active tab.</td></tr>`;
                // *** UPDATED: Call renamed/new functions with 0 on error ***
                updateBlankSupMissingCountDisplay(0);
                updateSupMissingCountDisplay(0);
                return;
            }
            const activeTabId = tabs[0].id;

            const messageToSend = { type: "REQUEST_SCAN_ACTIVE" };
            console.log("[Popup] Sending to active tab:", messageToSend);

            chrome.tabs.sendMessage(
                activeTabId,
                messageToSend,
                (response) => {
                    if (chrome.runtime.lastError) {
                        let msg = `Error connecting to active tab: ${chrome.runtime.lastError.message}`;
                        if (msg.includes("Receiving end does not exist")) {
                            msg += " Try reloading the page or check permissions.";
                        }
                        updateFooterStatus(msg, "error");
                        setActionButtonsState(false);
                        resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">${msg}</td></tr>`;
                        // *** UPDATED: Call renamed/new functions with 0 on error ***
                        updateBlankSupMissingCountDisplay(0);
                        updateSupMissingCountDisplay(0);
                        return;
                    }

                    if (response?.status === "success") {
                        updateFooterStatus("Scan complete. Processing results...");
                        console.log("Popup (Active Tab): Received scan results", response.data?.length || 0, "items.");
                        const currentScanResults = Array.isArray(response.data) ? response.data : [];

                        console.log("Popup (Active Tab): Comparing current scan against results loaded at init.");
                        const { comparisonOutput, mismatchCount } = compareResults(resultsLoadedAtInit, currentScanResults);

                        console.log("Popup (Active Tab): Displaying comparison results.");
                        // displayComparisonResults now calculates and updates both counts
                        displayComparisonResults(comparisonOutput, mismatchCount);

                        console.log("Popup (Active Tab): Saving current scan results to storage for next session.");
                        chrome.storage.local.set({ [STORAGE_KEY]: currentScanResults }, () => {
                            if (chrome.runtime.lastError) {
                                console.error("Error saving active tab results:", chrome.runtime.lastError.message);
                                updateFooterStatus(footerStatusDiv.textContent + " | Error saving results!", "error");
                            } else {
                                console.log("Active tab results saved successfully.");
                            }
                        });

                    } else if (response?.status === "error") {
                        updateFooterStatus(`Error during scan on page: ${response.message}`, "error");
                        setActionButtonsState(false);
                        resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">Scan failed on page: ${response.message}</td></tr>`;
                        // *** UPDATED: Call renamed/new functions with 0 on error ***
                        updateBlankSupMissingCountDisplay(0);
                        updateSupMissingCountDisplay(0);

                    } else {
                        updateFooterStatus("Warning: Unexpected response received from active tab.", "error");
                        console.warn("Unexpected response:", response);
                        setActionButtonsState(false);
                        resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">Received an unexpected response from the page.</td></tr>`;
                        // *** UPDATED: Call renamed/new functions with 0 on error ***
                        updateBlankSupMissingCountDisplay(0);
                        updateSupMissingCountDisplay(0);
                    }
                }
            );
            updateFooterStatus("Scan requested for active tab. Waiting for results...");

        } catch (error) {
            console.error("Popup (Active Tab): Init error:", error);
            updateFooterStatus("Error initializing active tab scan.", "error");
            resultsLoadedAtInit = [];
            setActionButtonsState(false);
            resultsBody.innerHTML = `<tr class="loading-placeholder"><td colspan="5">Initialization Error: ${error.message}</td></tr>`;
            updateUrlDisplay(null);
            // *** UPDATED: Call renamed/new functions with 0 on error ***
            updateBlankSupMissingCountDisplay(0);
            updateSupMissingCountDisplay(0);
        }

        // Add Button Listeners
        if (saveButton && !saveButton.hasAttribute('data-listener-added')) {
             saveButton.addEventListener('click', saveResultsToFile);
             saveButton.setAttribute('data-listener-added', 'true');
        }
        if (emailButton && !emailButton.hasAttribute('data-listener-added')) {
             emailButton.addEventListener('click', emailResults);
             emailButton.setAttribute('data-listener-added', 'true');
        }
    }

    initialize();
});