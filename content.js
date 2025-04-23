// --- content.js ---

console.info("[Superscript Finder] Content script loaded and ready."); // Use info for consistency

/**
 * Extracts the meaningful visible text from a superscript's outerHTML string.
 * (This should ideally be the same robust function used in results_summary.js/comparer_window.js)
 * @param {string} inputString The outerHTML string of the <sup> element.
 * @returns {string} The extracted visible text, or an empty string if none found.
 */
function extractVisibleSuperscriptText(inputString) {
    // --- Reuse the robust extraction logic from previous examples ---
    // (Make sure this function is defined here or imported if modularized)
    if (!inputString || typeof inputString !== 'string') {
        return '';
    }
    const trimmedInput = inputString.trim();

    // Handle cases where input is likely plain text already
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
        const supElement = doc.body.firstChild;

        if (!supElement || supElement.nodeName !== 'SUP') {
            const simpleMatch = inputString.match(/<sup>(.*?)<\/sup>/i);
            return simpleMatch ? simpleMatch[1].trim() : '';
        }

        let extractedText = supElement.innerText?.trim();

        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
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
            if (potentialText) extractedText = potentialText;
        }

        if (!extractedText || extractedText.includes('Opens a modal dialog')) {
            const clone = supElement.cloneNode(true);
            clone.querySelectorAll('.hidden, .sr-only, .visually-hidden, [aria-hidden="true"], [data-translation-text]').forEach(hiddenEl => hiddenEl.remove());
            let textAfterRemoval = clone.textContent?.trim() || '';
            if (textAfterRemoval) extractedText = textAfterRemoval;
        }

        return extractedText || '';

    } catch (e) {
        console.error("[extractText - content.js] Error parsing outerHTML:", e, "Input:", inputString);
        return '';
    }
}


/**
 * Checks if a superscript element is visually rendered on the same line as
 * the character immediately preceding it.
 * @param {HTMLElement} supElement The <sup> element to check.
 * @param {string} extractedText The text extracted from the sup element.
 * @returns {'ABOVE BASELINE' | 'SAMELINE' | 'BLANK' | 'CHECK_ERROR' | 'UNKNOWN'} String indicating the position.
 */
function checkSuperscriptPosition(supElement, extractedText) {
    if (!supElement) return 'CHECK_ERROR';

    const supRect = supElement.getBoundingClientRect();

    // If the element has no dimensions or extracted text is empty, classify position as BLANK
    if (extractedText === '' || supRect.width === 0 || supRect.height === 0) {
        return 'BLANK';
    }

    // Find the immediately preceding text node or suitable element node for baseline comparison
    let baselineNode = supElement.previousSibling;
    let baselineRect = null;

    while (baselineNode) {
        if (baselineNode.nodeType === Node.TEXT_NODE && baselineNode.textContent?.trim()) {
            // Get bounding box of the last character of the text node
            const range = document.createRange();
            try {
                const textLength = baselineNode.textContent.length;
                if (textLength > 0) {
                    range.setStart(baselineNode, textLength - 1);
                    range.setEnd(baselineNode, textLength);
                    const rects = range.getClientRects();
                    if (rects.length > 0) {
                        baselineRect = rects[rects.length - 1]; // Use the last rect for multi-line text nodes
                        break; // Found a suitable text node baseline
                    }
                }
            } catch (e) {
                 console.warn(`[Superscript Finder] Error getting range for text node:`, e, baselineNode);
            } finally {
                 range.detach();
            }
        } else if (baselineNode.nodeType === Node.ELEMENT_NODE) {
            // Check if the element is inline-like and visible
            const style = window.getComputedStyle(baselineNode);
            if (style.display !== 'block' && style.display !== 'none' && style.visibility !== 'hidden' && baselineNode.offsetParent !== null) {
                const elemRect = baselineNode.getBoundingClientRect();
                // Ensure the element has dimensions
                if (elemRect.width > 0 || elemRect.height > 0) {
                    baselineRect = elemRect; // Use the element's rect as baseline reference
                    break; // Found a suitable element node baseline
                }
            }
        }
        baselineNode = baselineNode.previousSibling;
    }

    // Perform the comparison if we found a valid baseline
    if (baselineRect && baselineRect.width > 0 && baselineRect.height > 0) {
        const tolerance = 2; // Pixel tolerance for rendering variations
        // Check if the bottom of the superscript is significantly above the bottom of the baseline element/text
        if (supRect.bottom < baselineRect.bottom - tolerance) {
            return 'ABOVE BASELINE';
        } else {
            // Otherwise, consider it on the same line (or close enough)
            return 'SAMELINE';
        }
    } else {
        // If no suitable preceding baseline could be found
        return 'UNKNOWN';
    }
}


/**
 * Finds all superscript elements on the page, extracts relevant information,
 * and determines their position relative to the preceding text baseline.
 * @returns {Array<object>} An array of objects, each representing a found superscript.
 */
/**
 * Finds all superscript elements on the page, extracts relevant information,
 * and determines their position relative to the preceding text baseline.
 * @returns {Array<object>} An array of objects, each representing a found superscript.
 */
function findSuperscripts() {
    console.info("[Superscript Finder] Starting scan on:", window.location.href);
    const results = [];
    let processingErrorOccurred = false;
    let uniqueIdCounter = 0; // Use a separate counter for reliable unique IDs

    try {
        const supElements = document.querySelectorAll('sup');
        console.log(`[Superscript Finder] Found ${supElements.length} sup elements.`);

        supElements.forEach((sup, index) => { // Keep original index for logging if needed
            try {
                const uniqueId = `superscript-finder-${uniqueIdCounter}`; // Use counter for ID
                sup.setAttribute('data-superscript-finder-id', uniqueId);
                uniqueIdCounter++; // Increment counter

                // --- Get Raw HTML and Extracted Text ---
                const rawSuperscriptHtml = sup.outerHTML; // Get the raw HTML
                // Extract text primarily needed for the position check now
                const extractedText = extractVisibleSuperscriptText(rawSuperscriptHtml);

                // --- Get Preceding Context ---
                let precedingText = '';
                let currentNode = sup.previousSibling;
                let charCount = 0;
                const maxChars = 50; // Limit context length
                while (currentNode && charCount < maxChars) {
                    let text = '';
                    if (currentNode.nodeType === Node.TEXT_NODE) {
                        // Get text content from text nodes
                        text = currentNode.textContent || '';
                    }
                    // Optional: Add handling for ELEMENT_NODE if needed, but keep it simple for now
                    // else if (currentNode.nodeType === Node.ELEMENT_NODE) {
                    //     text = (currentNode.textContent || '').trim();
                    // }

                    if (text) {
                        let availableChars = maxChars - charCount;
                        // Prepend the text, slicing if necessary
                        precedingText = text.slice(-availableChars) + precedingText;
                        charCount += text.length > availableChars ? availableChars : text.length;
                    }
                    // Move to the previous sibling
                    currentNode = currentNode.previousSibling;
                }
                // Clean up whitespace and add ellipsis if truncated
                precedingText = precedingText.replace(/\s+/g, ' ').trim();
                if (currentNode && charCount >= maxChars) { // Check if loop stopped due to maxChars
                    precedingText = '...' + precedingText;
                }

                // --- Define fullContext as ONLY the preceding text ---
                const fullContext = precedingText; // MODIFIED LINE
                // ---

                // --- Check Position ---
                let position = 'CHECK_ERROR'; // Default
                try {
                    // Pass extracted text to help determine 'BLANK' position
                    position = checkSuperscriptPosition(sup, extractedText);
                } catch (e) {
                    console.error(`[Superscript Finder] Error checking position for element index ${index}:`, e);
                }

                // --- Push results for ALL found sup tags ---
                results.push({
                    fullContext: fullContext,           // Now correctly contains only preceding text
                    superscriptText: rawSuperscriptHtml, // Still send raw HTML for parsing in results_summary
                    SupMissing: false,                   // Tag exists
                    position: position,
                    elementId: uniqueId
                });

            } catch (elementError) {
                console.error(`[Superscript Finder] CRITICAL ERROR processing sup element at index ${index}:`, elementError, "Element:", sup);
                processingErrorOccurred = true;
            }
        }); // --- End forEach ---

    } catch (scanError) {
        console.error("[Superscript Finder] CRITICAL ERROR during main scan execution:", scanError);
        processingErrorOccurred = true;
    }

    // Final logging
    if (processingErrorOccurred) {
        console.warn("[Superscript Finder] Scan completed, but errors occurred during processing. See logs above.");
    } else {
        console.info(`[Superscript Finder] Scan complete. Processed ${results.length} superscripts (including empty ones).`);
    }
    return results;
}


// --- Helper function to scroll and highlight ---
function scrollToElement(elementId) {
    try {
        const element = document.querySelector(`[data-superscript-finder-id="${elementId}"]`);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            element.style.transition = 'outline 0.1s ease-in-out, background-color 0.1s ease-in-out';
            element.style.outline = '3px solid #FF0000';
            element.style.backgroundColor = 'rgba(255, 255, 0, 0.3)';
            element.style.outlineOffset = '2px';
            setTimeout(() => {
                element.style.outline = '';
                element.style.backgroundColor = '';
                element.style.outlineOffset = '';
            }, 1500);
            console.log(`[Superscript Finder] Scrolled to element: ${elementId}`);
            return true;
        } else {
            console.warn(`[Superscript Finder] Element not found for scrolling: ${elementId}`);
            return false;
        }
    } catch (error) {
        console.error(`[Superscript Finder] Error scrolling to element ${elementId}:`, error);
        return false;
    }
}


// --- Combined Message Listener ---
// NOTE: Having multiple listeners for chrome.runtime.onMessage can cause issues.
// Use ONE listener that handles all expected message types.
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    console.log("[Content Script] Message received:", request); // Log all incoming requests

    switch (request.type) {
        // --- Handler for Background Multi-Scan ---
                // --- Handler for Background Multi-Scan ---
        case "REQUEST_SUPERSCRIPTS":
            console.log("[Superscript Finder] Received BACKGROUND request to find superscripts for:", request.sourceUrl);
            if (!request.sourceUrl) {
                console.error("[Superscript Finder] ERROR: Received BACKGROUND REQUEST_SUPERSCRIPTS without a sourceUrl!", request);
                return false; // Don't proceed
            }
            const resultsBg = findSuperscripts();
            console.log(`[Superscript Finder] Attempting to send ${resultsBg.length} results back to BACKGROUND for ${request.sourceUrl}`);
            try {
                // Send message to background WITHOUT the callback function
                chrome.runtime.sendMessage({
                    type: "SUPERSCRIPT_RESULTS_MULTI",
                    data: resultsBg,
                    sourceUrl: request.sourceUrl
                });
                // No callback means we don't expect a response, avoiding the error.

            } catch (sendError) {
                console.error("[Superscript Finder] CRITICAL ERROR calling chrome.runtime.sendMessage (to background):", sendError);
            }
            // Return false as the listener itself doesn't use sendResponse here
            return false;

        // --- Handler for Popup Active Tab Scan ---
        case "REQUEST_SCAN_ACTIVE":
            console.log("[Superscript Finder] Received POPUP request for active tab scan.");
            try {
                const resultsPopup = findSuperscripts();
                console.log("[Superscript Finder] Sending results back to POPUP:", resultsPopup);
                sendResponse({ status: "success", data: resultsPopup }); // Respond to popup
            } catch (error) {
                console.error("[Superscript Finder] Error processing POPUP request:", error);
                sendResponse({ status: "error", message: error.message }); // Send error to popup
            }
            // Return true because sendResponse is asynchronous
            // It signals that the response will be sent later (even if it happens quickly).
            return true;

        // --- Handler for PING ---
        case "PING":
            console.log("[Superscript Finder] Received PING");
            sendResponse({ status: "PONG" }); // Respond to ping
            return true; // Indicate async response

        // --- Handler for SCROLL_TO_SUPERSCRIPT ---
        case "SCROLL_TO_SUPERSCRIPT":
            console.log("[Superscript Finder] Received request to scroll to:", request.elementId);
            if (request.elementId) {
                const success = scrollToElement(request.elementId);
                sendResponse({ success: success }); // Respond about scroll success/failure
            } else {
                console.error("[Superscript Finder] Scroll request missing elementId.");
                sendResponse({ success: false, error: "Missing elementId" });
            }
            // Return true because sendResponse is asynchronous
            return true;

        default:
            // Optional: Handle unknown message types gracefully
            // console.warn("[Content Script] Received unknown message type:", request.type);
            // No response needed for unknown types
            break; // Exit switch
    }

    // If the switch didn't handle the message and return true,
    // return false or undefined implicitly for synchronous handling.
    // Returning false explicitly can sometimes help avoid ambiguity.
    return false;
});