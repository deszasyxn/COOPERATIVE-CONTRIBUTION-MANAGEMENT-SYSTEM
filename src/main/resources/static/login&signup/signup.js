// ============================================================
// signup.js: checks if the cooperative already exists, then
// sanitizes and sends the signup data to the backend
// ============================================================

// ---- Settings: change these to match your backend ----------
const CONFIG = {
  checkEndpoint: "/api/cooperatives/exists",   // GET ?name=...  ->  { "exists": true | false }
  signupEndpoint: "/api/signup",               // POST JSON
  redirectTo: "login.html",                    // where to go after a successful signup
  debounceMs: 500,                             // wait after typing stops before checking
  timeoutMs: 10000
};

// Letters (any language), numbers, spaces and . , ' & ( ) -
const COOP_PATTERN  = /^[\p{L}\p{N} .,'&()\-]+$/u;
const ADMIN_PATTERN = /^[\p{L} .'\-]+$/u;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

// ---- Sanitizers --------------------------------------------
function cleanText(value) {
  return String(value)
    .normalize("NFKC")
    .replace(/[\u0000-\u001F\u007F-\u009F]/g, "")   // control characters
    .replace(/\s+/g, " ")                             // collapse extra spaces
    .trim();
}

const sanitizeCoop  = cleanText;
const sanitizeAdmin = cleanText;
const sanitizeEmail = function (v) { return cleanText(v).toLowerCase(); };

function validCoop(v)  { return v.length >= 3 && v.length <= 100 && COOP_PATTERN.test(v); }
function validAdmin(v) { return v.length >= 2 && v.length <= 80  && ADMIN_PATTERN.test(v); }
function validEmail(v) { return v.length <= 254 && EMAIL_PATTERN.test(v); }

// ---- Elements ----------------------------------------------
const form       = document.getElementById("signup-form");
const coopInput  = form.elements["fullname"];     // Cooperative Name
const adminInput = form.elements["adminname"];
const emailInput = form.elements["email"];
const termsBox   = form.elements["terms"];
const submitBtn  = form.querySelector(".btn");

// ---- Field state helper ------------------------------------
// state: "invalid" | "checking" | "available" | "clear"
function setFieldState(input, state, message) {
  const field = input.closest(".field");
  const err = field.querySelector(".error");
  field.classList.remove("invalid", "checking", "available");
  if (state !== "clear") field.classList.add(state);
  if (message) err.textContent = message;
}

function showFormError(message) {
  let box = form.querySelector(".form-error");
  if (!box) {
    box = document.createElement("p");
    box.className = "form-error";
    box.setAttribute("role", "alert");
    submitBtn.before(box);
  }
  box.textContent = message;
  box.hidden = !message;
}

// ---- Cooperative existence check ---------------------------
let checkController = null;   // lets us cancel an outdated request
let lastResult = null;        // { name, exists } for the last finished check
let debounceTimer = null;

async function cooperativeExists(name) {
  if (checkController) checkController.abort();
  checkController = new AbortController();
  const controller = checkController;
  const timer = setTimeout(function () { controller.abort(); }, CONFIG.timeoutMs);

  try {
    const res = await fetch(CONFIG.checkEndpoint + "?name=" + encodeURIComponent(name), {
      method: "GET",
      headers: { "Accept": "application/json" },
      credentials: "same-origin",
      signal: controller.signal
    });
    if (!res.ok) throw new Error("Bad response: " + res.status);

    const data = await res.json();
    if (typeof data.exists !== "boolean") throw new Error("Unexpected response");
    return data.exists;
  } finally {
    clearTimeout(timer);
  }
}

// Checks the name and updates the field. Returns "available", "exists",
// "invalid", "error" or "stale" (the user kept typing, so ignore it).
async function runCheck() {
  const name = sanitizeCoop(coopInput.value);

  if (!validCoop(name)) {
    lastResult = null;
    setFieldState(
      coopInput,
      "invalid",
      name
        ? "Use 3 to 100 letters, numbers, spaces and . , ' & ( ) - only."
        : "Enter the cooperative name."
    );
    return "invalid";
  }

  // Reuse the answer if we already checked this exact name
  if (lastResult && lastResult.name === name) {
    return showResult(lastResult.exists);
  }

  setFieldState(coopInput, "checking", "Checking if this cooperative is registered...");

  try {
    const exists = await cooperativeExists(name);
    if (sanitizeCoop(coopInput.value) !== name) return "stale";
    lastResult = { name: name, exists: exists };
    return showResult(exists);
  } catch (err) {
    if (err.name === "AbortError" && sanitizeCoop(coopInput.value) !== name) return "stale";
    lastResult = null;
    setFieldState(coopInput, "invalid", "We couldn't verify this name. Check your connection and try again.");
    return "error";
  }
}

function showResult(exists) {
  if (exists) {
    setFieldState(coopInput, "invalid", "A cooperative with this name is already registered.");
    return "exists";
  }
  setFieldState(coopInput, "available", "This cooperative name is available.");
  return "available";
}

// Live check: after typing stops, and when leaving the field
coopInput.addEventListener("input", function () {
  clearTimeout(debounceTimer);
  lastResult = null;
  showFormError("");
  setFieldState(coopInput, "clear");

  if (sanitizeCoop(coopInput.value).length >= 3) {
    debounceTimer = setTimeout(runCheck, CONFIG.debounceMs);
  }
});

coopInput.addEventListener("blur", function () {
  clearTimeout(debounceTimer);
  if (coopInput.value.trim()) runCheck();
});

// Clear errors on the other fields as the user types
[adminInput, emailInput, termsBox].forEach(function (el) {
  el.addEventListener("input", function () {
    setFieldState(el, "clear");
    showFormError("");
  });
});

// ---- Submit ------------------------------------------------
form.addEventListener("submit", async function (e) {
  e.preventDefault();
  showFormError("");
  clearTimeout(debounceTimer);

  const admin = sanitizeAdmin(adminInput.value);
  const email = sanitizeEmail(emailInput.value);

  // Validate the simple fields first
  const adminBad = !validAdmin(admin);
  const emailBad = !validEmail(email);
  if (adminBad) setFieldState(adminInput, "invalid", "Enter the admin's name (letters only).");
  if (emailBad) setFieldState(emailInput, "invalid", "Enter a valid email address.");

  const termsBad = !termsBox.checked;
  if (termsBad) showFormError("Please agree to the Terms and Conditions.");

  submitBtn.disabled = true;
  submitBtn.textContent = "CHECKING...";

  try {
    // Make sure the cooperative name is checked right now
    const status = await runCheck();
    if (status === "stale") return;
    if (status !== "available" || adminBad || emailBad || termsBad) return;

    submitBtn.textContent = "SIGNING UP...";

    const res = await fetch(CONFIG.signupEndpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      // If your backend uses a CSRF token, add it here:
      // headers: { ..., "X-CSRF-Token": document.querySelector('meta[name="csrf-token"]').content },
      body: JSON.stringify({
        cooperativeName: sanitizeCoop(coopInput.value),
        adminName: admin,
        email: email,
        agreedToTerms: true
      }),
      credentials: "same-origin"
    });

    if (res.ok) {
      window.location.href = CONFIG.redirectTo;
    } else if (res.status === 409) {
      // Someone registered this name between our check and the submit
      lastResult = { name: sanitizeCoop(coopInput.value), exists: true };
      showResult(true);
    } else if (res.status === 429) {
      showFormError("Too many attempts. Please wait a moment and try again.");
    } else {
      showFormError("Something went wrong on our end. Please try again.");
    }
  } catch (err) {
    showFormError("Can't reach the server. Check your connection and try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "SIGN UP";
  }
});