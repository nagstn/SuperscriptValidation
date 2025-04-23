// --- background.js ---

// --- Configuration & State ---
const SCAN_INTERVAL_MINUTES = 1500; // How often to run the scan (Set to your desired interval)
const URLS_TO_SCAN = [ // List of URLs to scan
  "https://www.wellsfargo.com/checking/prime/",
"https://www.wellsfargo.com/checking/premier/",
"https://www.wellsfargo.com/savings-cds/way2save/",
"https://www.wellsfargo.com/savings-cds/platinum/",
"https://www.wellsfargo.com/savings-cds/certificate-of-deposit/",
"https://www.wellsfargo.com/checking/",	
"https://www.wellsfargo.com/savings-cds/",
"https://www.wellsfargo.com/savings-cds/certificate-of-deposit/apply/",
"https://www.wellsfargo.com/investing/retirement/ira/select/destination/rates/",
"https://www.wellsfargo.com/savings-cds/rates/",
"https://web.secure.wellsfargo.com/product-selector/checking-results",
"file:///C:/Users/nagar/OneDrive/Desktop/Demo_superscript_react_test.html"
    // Add other URLs here
];
const MAX_CONCURRENT_SCANS = 3; // Limit simultaneous tab operations
const TAB_LOAD_TIMEOUT_MS = 30000; // Max time to wait for a tab to load
const PING_TIMEOUT_MS = 1000; // Max time to wait for content script ping response

// Storage Keys
const CURRENT_RESULTS_KEY = "multiPageScanResults_Current"; // Key for results of the *last completed* scan
const PREVIOUS_RESULTS_KEY = "multiPageScanResults_Previous"; // Key for results of the scan *before* the last completed one
const STATUS_STORAGE_KEY = "multiPageScanStatus";
// Key for the final results that results_Summary.html should read
const FINAL_RESULTS_KEY = CURRENT_RESULTS_KEY; // Use the same key for simplicity, summary reads the latest results

let scanQueue = [];
let pendingScans = 0;
let currentScanResults = {}; // Holds results *during* the current scan run
let scanInProgress = false;
let createdTabIds = new Set(); // Keep track of tabs we created

console.log("Background Script v3.3 Started (Previous Results Comparison).");

// --- Scheduling ---
function scheduleScan() {
    console.log(`[Scheduler] Scheduling next scan in ${SCAN_INTERVAL_MINUTES} minutes (repeating).`);
    // Clear just in case before creating, although onInstalled should handle the main reset
    chrome.alarms.clear("multiPageScanAlarm", (wasCleared) => {
        chrome.alarms.create("multiPageScanAlarm", {
            delayInMinutes: SCAN_INTERVAL_MINUTES, // Delay for the *next* run
            periodInMinutes: SCAN_INTERVAL_MINUTES // Make it repeat every interval
        });
        updateStatus({ running: false, scheduledTime: Date.now() + SCAN_INTERVAL_MINUTES * 60000 });
    });
}

// This function is called by onStartup, primarily useful if browser starts without update
function setupInitialSchedule() {
    chrome.alarms.get("multiPageScanAlarm", (alarm) => {
        if (!alarm) {
            console.log("[Scheduler] No existing alarm found on startup check. Scheduling scan.");
            // Use scheduleScan which now sets the period
            scheduleScan();
        } else {
            console.log("[Scheduler] Existing alarm found on startup check:", alarm);
            // If an alarm exists, we assume onInstalled handled setting the correct period,
            // or it's the correct one from a previous session.
        }
    });
}

// Listen for extension startup or update
chrome.runtime.onInstalled.addListener(async (details) => { // Make the listener async
    console.log(`[Scheduler] Extension ${details.reason}: Re-evaluating multi-scan schedule.`);

    // --- Clear any existing alarm first ---
    try {
        await chrome.alarms.clear("multiPageScanAlarm");
        console.log("[Scheduler] Cleared any existing 'multiPageScanAlarm'.");
    } catch (error) {
        // This might happen if the alarm didn't exist, usually safe to ignore
        console.warn("[Scheduler] Error trying to clear alarm (might be okay):", error);
    }
    // --- End Clear ---

    // --- Now schedule a new alarm with the current interval ---
    console.log(`[Scheduler] Scheduling new scan with interval: ${SCAN_INTERVAL_MINUTES} minutes (repeating).`);
    // Use delayInMinutes for the *first* run after installation/update
    // Use periodInMinutes to make it repeat
    chrome.alarms.create("multiPageScanAlarm", {
        // Use a short delay for the very first run after install/update for quicker feedback
        delayInMinutes: 0.1, // e.g., 6 seconds
        periodInMinutes: SCAN_INTERVAL_MINUTES // Set the repeating period
    });
    // --- End Schedule ---

    // Update status to reflect the new schedule
    updateStatus({ running: false, scheduledTime: Date.now() + 0.1 * 60000 });

});


// Listen for alarm
chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === "multiPageScanAlarm") {
        console.log("[Scheduler] Alarm triggered: Starting multi-page scan process.");
        startMultiPageScan();
    }
});

// --- Scan Orchestration ---
async function startMultiPageScan() {
    if (scanInProgress) {
        console.warn("[Multi-Scan] Scan attempt while another is in progress. Skipping.");
        return;
    }
    scanInProgress = true;
    pendingScans = 0;
    scanQueue = [...URLS_TO_SCAN]; // Reset queue
    currentScanResults = {}; // Reset results for this run
    createdTabIds.clear(); // Clear tracked tabs

    console.log("[Multi-Scan] Starting multi-page scan for URLs:", URLS_TO_SCAN);
    updateStatus({ running: true, startTime: Date.now(), totalUrls: URLS_TO_SCAN.length, completedUrls: 0 });

    // Preserve previous results before overwriting
    try {
        const data = await chrome.storage.local.get(CURRENT_RESULTS_KEY);
        if (data[CURRENT_RESULTS_KEY]) {
            await chrome.storage.local.set({ [PREVIOUS_RESULTS_KEY]: data[CURRENT_RESULTS_KEY] });
            console.log("[Multi-Scan] Previous results preserved.");
        } else {
            console.log("[Multi-Scan] No previous results found to preserve.");
            // Ensure PREVIOUS_RESULTS_KEY is cleared if no current results exist
            await chrome.storage.local.remove(PREVIOUS_RESULTS_KEY);
        }
    } catch (error) {
        console.error("[Multi-Scan] Error preserving previous results:", error);
    }

    // Start processing the queue
    processScanQueue();
}

function processScanQueue() {
    while (pendingScans < MAX_CONCURRENT_SCANS && scanQueue.length > 0) {
        const url = scanQueue.shift();
        if (url) {
            pendingScans++;
            console.log(`[Multi-Scan Worker] Starting processing for: ${url}. Queue size: ${scanQueue.length}, Pending: ${pendingScans}`);
            // Don't await here, let them run concurrently
            processUrl(url);
        }
    }
    if (pendingScans === 0 && scanQueue.length === 0 && scanInProgress) {
        // This condition might be reached if the initial queue was empty or all failed immediately
        console.log("[Multi-Scan] Queue processed (or was empty).");
        completeScan(); // Pass the (likely empty) currentScanResults
    }
}

function decrementPendingAndCheckCompletion(url) {
    // Use a placeholder if URL is somehow invalid to avoid errors accessing it
    const urlKey = url || `invalid_url_pending_${pendingScans}`;
    pendingScans--;
    console.log(`[Multi-Scan Worker] Finished processing for ${urlKey}. Pending: ${pendingScans}`);
    // Update status with completed count
    const completedCount = Object.keys(currentScanResults).length;
    updateStatus({ completedUrls: completedCount });

    if (pendingScans < MAX_CONCURRENT_SCANS && scanQueue.length > 0) {
        processScanQueue(); // Process next item if slots available
    } else if (pendingScans === 0 && scanQueue.length === 0 && scanInProgress) {
        console.log("[Multi-Scan] All scans completed or failed.");
        completeScan(); // Pass the final accumulated currentScanResults
    }
}

// --- Updated completeScan function ---
async function completeScan() {
    if (!scanInProgress) return; // Prevent double completion

    console.log("[Multi-Scan] Scan process finished. Saving final results.");
    const finalResultsToSave = { ...currentScanResults }; // Capture results at completion time
    scanInProgress = false; // Mark scan as not in progress *before* async operations

    try {
        // Save the final results under the designated key
        await chrome.storage.local.set({ [FINAL_RESULTS_KEY]: finalResultsToSave });
        console.log(`[Multi-Scan] Final results saved under key: ${FINAL_RESULTS_KEY}.`);
        updateStatus({ running: false, completedTime: Date.now(), results: finalResultsToSave });

        // --- Show notification ---
        showScanCompleteNotification(finalResultsToSave);
        // --- End notification ---

        // --- Open the results summary page ---
        try {
            const summaryPageUrl = chrome.runtime.getURL("results_Summary.html");
            await chrome.tabs.create({ url: summaryPageUrl, active: true }); // Make it active
            console.log("[Multi-Scan] Opened results_Summary.html");
        } catch (error) {
            console.error("[Multi-Scan] Error opening results_Summary.html:", error);
            // Optionally notify the user that the report couldn't be opened
        }
        // --- End open summary page ---

    } catch (error) {
        console.error("[Multi-Scan] Error saving final results:", error);
        updateStatus({ running: false, error: "Failed to save final results." });
        // Consider if you still want to show notification/schedule next scan on save error
        showScanCompleteNotification({}); // Show a generic notification maybe?
    } finally {
        cleanupCreatedTabs(); // Clean up any tabs we opened
        scheduleScan(); // Schedule the next run regardless of save/open success
    }
}
// --- End Updated completeScan function ---

// --- URL Processing & Content Script Interaction ---
async function processUrl(url) {
    let tabId = null;
    let createdThisTab = false;
    try {
        console.log(`[Multi-Scan Worker] Attempting to open or find tab for: ${url}`);

        // Always create a new background tab for simplicity and isolation
        console.log(`[Multi-Scan Worker] Creating new background tab for ${url}.`);
        const newTab = await chrome.tabs.create({ url: url, active: false });
        tabId = newTab.id;
        if (!tabId) throw new Error("Failed to create tab.");
        createdThisTab = true;
        createdTabIds.add(tabId); // Track the tab we created
        console.log(`[Multi-Scan Worker] Successfully created new tab with ID: ${tabId} for ${url}`);

        console.log(`[Multi-Scan Worker] Waiting for tab ${tabId} (${url}) to load...`);
        await waitForTabLoad(tabId, TAB_LOAD_TIMEOUT_MS);
        console.log(`[Multi-Scan Worker] Tab ${tabId} loaded. Injecting and scanning...`);
        await injectAndScan(tabId, url); // Pass the valid URL

    } catch (error) {
        console.error(`[Multi-Scan Worker] Error processing URL ${url} (Tab ${tabId}):`, error);
        storeResult(url, { status: 'error', error: error.message || 'Unknown processing error' });
        if (tabId && createdThisTab) {
            closeTabIfNecessary(tabId, url); // Close tab if we created it and it failed
        }
        decrementPendingAndCheckCompletion(url); // Ensure completion check happens on error
    }
    // Note: successful scans will call decrementPendingAndCheckCompletion via the result handler
}

function waitForTabLoad(tabId, timeoutMs) {
    console.log(`[Multi-Scan Worker] Waiting for tab ${tabId} load (max ${timeoutMs}ms)...`);
    return new Promise((resolve, reject) => {
        let listener = null; // Declare listener variable

        const timeoutHandle = setTimeout(() => {
            console.warn(`[Multi-Scan Worker] Tab ${tabId} load timed out after ${timeoutMs}ms.`);
            if (listener) { // Check if listener exists before removing
                chrome.tabs.onUpdated.removeListener(listener);
            }
            reject(new Error(`Tab load timed out after ${timeoutMs}ms`));
        }, timeoutMs);

        listener = (updatedTabId, changeInfo) => { // Assign the function to listener
            if (updatedTabId === tabId && changeInfo.status === 'complete') {
                console.log(`[Multi-Scan Worker] Tab ${tabId} reached status 'complete'.`);
                clearTimeout(timeoutHandle);
                chrome.tabs.onUpdated.removeListener(listener); // Remove listener *before* resolving
                resolve();
            }
        };

        chrome.tabs.onUpdated.addListener(listener);

        // Initial check in case the tab loaded extremely quickly
        chrome.tabs.get(tabId, (tab) => {
            if (chrome.runtime.lastError) {
                 clearTimeout(timeoutHandle);
                 if (listener) { chrome.tabs.onUpdated.removeListener(listener); }
                 reject(new Error(`Failed to get tab ${tabId}: ${chrome.runtime.lastError.message}`));
            } else if (tab && tab.status === 'complete') {
                console.log(`[Multi-Scan Worker] Tab ${tabId} was already complete.`);
                clearTimeout(timeoutHandle);
                if (listener) { chrome.tabs.onUpdated.removeListener(listener); }
                resolve();
            }
        });
    });
}

// --- PING FUNCTION WITH MANUAL TIMEOUT ---
async function pingContentScript(tabId, timeoutMs = PING_TIMEOUT_MS) {
    console.log(`[BG Ping] Pinging tab ${tabId}...`);
    return new Promise((resolve, reject) => {
        const timeoutHandle = setTimeout(() => {
            console.warn(`[BG Ping] Ping timed out for tab ${tabId} after ${timeoutMs}ms.`);
            reject(new Error(`Ping timed out after ${timeoutMs}ms`));
        }, timeoutMs);

        // Send the message
        chrome.tabs.sendMessage(tabId, { type: "PING" }, (response) => {
            clearTimeout(timeoutHandle); // Clear the timeout if we get a response

            // Check for runtime errors (e.g., no content script, tab closed)
            if (chrome.runtime.lastError) {
                console.warn(`[BG Ping] Ping failed for tab ${tabId}:`, chrome.runtime.lastError.message);
                reject(new Error(chrome.runtime.lastError.message));
            } else if (response && response.status === "PONG") {
                console.log(`[BG Ping] Received PONG from tab ${tabId}.`);
                resolve(true); // Content script is active
            } else {
                // This case handles scenarios where the content script exists but doesn't send PONG
                console.warn(`[BG Ping] Received unexpected response or no response from tab ${tabId}:`, response);
                reject(new Error("Unexpected or no ping response"));
            }
        });
    });
}
// --- END PING FUNCTION ---

// --- INJECT AND SCAN FUNCTION (Using new Ping & URL Check) ---
async function injectAndScan(tabId, url) { // url parameter is crucial
    console.log(`[Multi-Scan Worker] Attempting injection and scan for tab ${tabId} (${url})`);
    try {
        // Try pinging first using the function with manual timeout
        await pingContentScript(tabId);
        console.log(`[Multi-Scan Worker] Content script already active in tab ${tabId}. Requesting scan.`);

        // --- URL Check before sending message ---
        console.log(`[Multi-Scan Worker] Preparing to send REQUEST_SUPERSCRIPTS. URL is: ${url}`);
        if (!url) {
            console.error(`[Multi-Scan Worker] CRITICAL: Attempting to send REQUEST_SUPERSCRIPTS with an invalid URL for tab ${tabId}. Aborting message send.`);
            storeResult(url || `invalid_url_tab_${tabId}`, { status: 'error', error: 'Background script had invalid URL before sending request.' });
            decrementPendingAndCheckCompletion(url || `invalid_url_tab_${tabId}`);
            closeTabIfNecessary(tabId, url);
            return; // Stop before sending the message
        }
        // --- End URL Check ---

        // Content script exists, just send the request
        chrome.tabs.sendMessage(tabId, { type: "REQUEST_SUPERSCRIPTS", sourceUrl: url });

    } catch (pingError) {
        // Ping failed (timeout or other error like "no receiving end"), assume injection is needed
        console.warn(`[Multi-Scan Worker] Ping failed for tab ${tabId} (${pingError.message}), attempting injection...`);
        try {
            await chrome.scripting.executeScript({
                target: { tabId: tabId },
                files: ['content.js']
            });
            console.log(`[Multi-Scan Worker] Successfully injected content script into tab ${tabId}. Requesting scan.`);
            // Wait a very brief moment for the script to potentially initialize its listener
            await new Promise(resolve => setTimeout(resolve, 150));

            // --- URL Check before sending message (post-injection) ---
            console.log(`[Multi-Scan Worker] Preparing to send REQUEST_SUPERSCRIPTS post-injection. URL is: ${url}`);
            if (!url) {
                console.error(`[Multi-Scan Worker] CRITICAL: Attempting to send REQUEST_SUPERSCRIPTS post-injection with an invalid URL for tab ${tabId}. Aborting message send.`);
                storeResult(url || `invalid_url_tab_${tabId}`, { status: 'error', error: 'Background script had invalid URL before sending request post-injection.' });
                decrementPendingAndCheckCompletion(url || `invalid_url_tab_${tabId}`);
                closeTabIfNecessary(tabId, url);
                return; // Stop before sending the message
            }
             // --- End URL Check ---

            // Send the request after injection
            chrome.tabs.sendMessage(tabId, { type: "REQUEST_SUPERSCRIPTS", sourceUrl: url }, (response) => {
                 // Check for errors sending the message *after* injection
                 if (chrome.runtime.lastError) {
                     console.error(`[Multi-Scan Worker] Error sending REQUEST_SUPERSCRIPTS to tab ${tabId} after injection:`, chrome.runtime.lastError.message);
                     // Handle this as a scan failure
                     storeResult(url, { status: 'error', error: `Failed to communicate post-injection: ${chrome.runtime.lastError.message}` });
                     decrementPendingAndCheckCompletion(url);
                     closeTabIfNecessary(tabId, url);
                 }
                 // No specific response needed from REQUEST_SUPERSCRIPTS itself
            });
        } catch (injectionError) {
            console.error(`[Multi-Scan Worker] Failed to inject content script into tab ${tabId}:`, injectionError);
            storeResult(url, { status: 'error', error: `Failed to inject script: ${injectionError.message}` });
            decrementPendingAndCheckCompletion(url);
            closeTabIfNecessary(tabId, url);
        }
    }
}
// --- END INJECT AND SCAN FUNCTION ---


// --- Result Handling ---
chrome.runtime.onMessage.addListener((message, sender) => {
    // Only process results if a scan is actually marked as in progress
    if (scanInProgress && message.type === "SUPERSCRIPT_RESULTS_MULTI") {
        const url = message.sourceUrl; // Get URL from the message
        const tabId = sender.tab?.id;
        console.log(`[Result Handler] Received results for URL: ${url} from sender tab: ${tabId}`);

        if (!url) {
            console.error("[Result Handler] Received result with UNDEFINED URL. Ignoring.", message.data, sender);
            // Cannot store result without URL. This might leave a scan pending indefinitely if not handled.
            // Consider if decrementPendingAndCheckCompletion should be called here with a placeholder? Risky.
            return false; // Ignore if URL is missing
        }

        if (currentScanResults.hasOwnProperty(url)) {
             console.warn(`[Result Handler] Received duplicate result for ${url}. Ignoring.`);
             return false; // Avoid processing duplicates
        }

        storeResult(url, { status: 'success', data: message.data });
        decrementPendingAndCheckCompletion(url); // Use the valid URL
        closeTabIfNecessary(tabId, url); // Close the tab now that we have results

    } else if (message.type === "SUPERSCRIPT_RESULTS_MULTI") {
        // Received results but scan isn't marked as in progress (e.g., from a previous run's leftover message)
        console.warn(`[Result Handler] Received result for ${message.sourceUrl}, but no scan is in progress. Ignoring.`);
    }
    // Keep listener alive for potential other message types if needed
    // Return true only if using sendResponse asynchronously elsewhere in this listener
    return false;
});

function storeResult(url, result) {
    // Use a placeholder key if the URL is somehow invalid to prevent errors
    const key = url || `invalid_url_result_${Date.now()}`;
    // Store result only if scan is in progress
    if (scanInProgress) {
        currentScanResults[key] = result;
        console.log(`[Result Handler] Stored result for ${key}. Status: ${result.status}`);
        // Update status immediately after storing a result
        const completedCount = Object.keys(currentScanResults).length;
        updateStatus({ completedUrls: completedCount });
    } else {
        console.warn(`[Result Handler] Attempted to store result for ${key}, but scan is not in progress. Ignoring.`);
    }
}

// --- Status Update ---
async function updateStatus(statusUpdate) {
    try {
        // Retrieve current status, provide default if not found
        const currentStatus = (await chrome.storage.local.get(STATUS_STORAGE_KEY))[STATUS_STORAGE_KEY] || { running: false };
        const newStatus = { ...currentStatus, ...statusUpdate };
        await chrome.storage.local.set({ [STATUS_STORAGE_KEY]: newStatus });
        // console.log("[Status Update] Status updated:", newStatus); // Optional: Verbose logging
    } catch (error) {
        console.error("[Status Update] Error updating status:", error);
    }
}

// --- Notification Function (Corrected) ---
function showScanCompleteNotification(results) {
    const resultsArray = Object.values(results || {}); // Handle case where results might be null/undefined
    // Define issue: error status OR success status with data containing non-ABOVE BASELINE positions
    const issueCount = resultsArray.filter(r =>
        r.status === 'error' ||
        (r.status === 'success' && r.data && r.data.some(item => item.position !== 'ABOVE BASELINE'))
    ).length;
    const totalUrls = resultsArray.length;

    // Define all required properties directly and ensure they have values
    const notificationOptions = {
        type: 'basic', // REQUIRED
        iconUrl: 'icons/icon128.png', // REQUIRED - Verify this path exists
        title: 'Superscript Scan Complete', // REQUIRED
        message: `Scanned ${totalUrls} URLs. ${issueCount > 0 ? `${issueCount} URLs have issues or errors.` : 'No issues found.'}` // REQUIRED
    };

    console.log("Attempting to create notification with options:", notificationOptions);

    // Check manifest permissions for "notifications"
    chrome.notifications.create(notificationOptions, (notificationId) => {
        // The callback is optional for create, but good for logging errors
        if (chrome.runtime.lastError) {
            console.error("Notification creation failed:", chrome.runtime.lastError.message, "Options were:", notificationOptions);
        } else {
            console.log("Notification shown successfully:", notificationId);
        }
    });
}
// --- End Notification Function ---


// --- Tab Management ---
function closeTabIfNecessary(tabId, url) {
    // Only close tabs that we created and tracked
    if (tabId && createdTabIds.has(tabId)) {
        console.log(`[Tab Management] Closing tab ${tabId} created for ${url}.`);
        chrome.tabs.remove(tabId, () => {
            if (chrome.runtime.lastError) {
                // Ignore error if tab was already closed (e.g., manually by user)
                // console.warn(`[Tab Management] Error closing tab ${tabId}:`, chrome.runtime.lastError.message);
            }
            createdTabIds.delete(tabId); // Remove from tracked set after attempting removal
        });
    } else if (tabId) {
         console.log(`[Tab Management] Not closing tab ${tabId} as it was not created by this scan.`);
    }
}

function cleanupCreatedTabs() {
    // This function is called at the very end of the scan process
    if (createdTabIds.size > 0) {
        console.log("[Tab Management] Cleaning up potentially orphaned created tabs:", Array.from(createdTabIds));
        createdTabIds.forEach(tabId => {
            chrome.tabs.remove(tabId, () => {
                if (chrome.runtime.lastError) { /* Ignore errors, tab might already be gone */ }
            });
        });
        createdTabIds.clear(); // Clear the set after attempting cleanup
    }
}

// --- Initial Setup ---
// onInstalled listener handles setup on install/update.
// setupInitialSchedule handles the case where the browser starts without an update.
setupInitialSchedule();