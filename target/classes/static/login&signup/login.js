// ============================================================
// login.js: sanitize, validate and send login data to the backend
// ============================================================

// ---- Settings: change these to match your backend ----------
const CONFIG = {
  endpoint: "/api/auth/login",
  redirectTo: "/member/memberDash.html",
  timeoutMs: 10000
};

const LIMITS = {
  userMin: 3,
  userMax: 254,      // max length of an email address
  passMax: 128
};

// Letters, numbers and . _ - @ + only (covers usernames and emails)
const USER_PATTERN = /^[A-Za-z0-9._@+\-]+$/;

// ---- Show / hide password ----------------------------------
document.querySelectorAll(".toggle-pw").forEach(function (btn) {
  btn.addEventListener("click", function () {
    const input = document.getElementById(btn.getAttribute("aria-controls"));
    const showing = input.type === "text";
    input.type = showing ? "password" : "text";
    btn.classList.toggle("showing", !showing);
    btn.setAttribute("aria-label", showing ? "Show password" : "Hide password");
  });
});

// ---- Sanitizers --------------------------------------------
// Username / email: normalize, strip control characters, trim.
function sanitizeUsername(value) {
  return String(value)
    .normalize("NFKC")
    .replace(/[\u0000-\u001F\u007F-\u009F]/g, "")
    .trim();
}

// Password: never trimmed or altered, because changing it would break
// the login. Only the length and null bytes are checked.
function isValidPassword(value) {
  return value.length > 0 && value.length <= LIMITS.passMax && !value.includes("\u0000");
}

function isValidUsername(value) {
  return (
    value.length >= LIMITS.userMin &&
    value.length <= LIMITS.userMax &&
    USER_PATTERN.test(value)
  );
}

// ---- UI helpers --------------------------------------------
function setFieldInvalid(input, invalid, message) {
  const field = input.closest(".field");
  if (!field) return;
  field.classList.toggle("invalid", invalid);
  const err = field.querySelector(".error");
  if (err && message) err.textContent = message;
}

function getFormError(form) {
  let box = form.querySelector(".form-error");
  if (!box) {
    box = document.createElement("p");
    box.className = "form-error";
    box.setAttribute("role", "alert");
    box.hidden = true;
    form.querySelector(".btn").before(box);
  }
  return box;
}

function showFormError(form, message) {
  const box = getFormError(form);
  box.textContent = message;
  box.hidden = !message;
}

function setLoading(form, loading) {
  const btn = form.querySelector(".btn");
  btn.disabled = loading;
  btn.textContent = loading ? "LOGGING IN..." : "LOGIN";
}

// ---- Send to backend ---------------------------------------
async function sendLogin(payload) {
  const controller = new AbortController();
  const timer = setTimeout(function () { controller.abort(); }, CONFIG.timeoutMs);

  try {
    const res = await fetch(CONFIG.endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
        // If your backend uses a CSRF token, add it here:
        // "X-CSRF-Token": document.querySelector('meta[name="csrf-token"]').content
      },
      body: JSON.stringify(payload),
      credentials: "same-origin",   // use "include" if the backend is on another domain
      signal: controller.signal
    });

    let data = null;
    try { data = await res.json(); } catch (_) { /* response had no JSON body */ }

    return { ok: res.ok, status: res.status, data: data };
  } finally {
    clearTimeout(timer);
  }
}

// ---- Form submit -------------------------------------------
const form = document.getElementById("login-form");

form.addEventListener("submit", async function (e) {
  e.preventDefault();
  showFormError(form, "");

  const userInput = form.elements["username"];
  const passInput = form.elements["password"];

  const username = sanitizeUsername(userInput.value);
  const password = passInput.value;           // left exactly as typed
  const remember = form.elements["remember"].checked;

  // Validate
  const userBad = !isValidUsername(username);
  const passBad = !isValidPassword(password);

  setFieldInvalid(
    userInput,
    userBad,
    username ? "Use letters, numbers and . _ - @ + only." : "Enter your username or email."
  );
  setFieldInvalid(passInput, passBad, "Enter your password.");

  if (userBad || passBad) return;

  // Send
  setLoading(form, true);
  try {
    const result = await sendLogin({ username: username, password: password, remember: remember });

    if (result.ok) {
      window.location.href = (result.data && result.data.redirect) || CONFIG.redirectTo;
      return;
    }

    // Generic messages on purpose, so attackers can't tell
    // whether the username exists.
    if (result.status === 400 || result.status === 401 || result.status === 403) {
      showFormError(form, "Incorrect username or password.");
    } else if (result.status === 429) {
      showFormError(form, "Too many attempts. Please wait a moment and try again.");
    } else {
      showFormError(form, "Something went wrong on our end. Please try again.");
    }
    passInput.value = "";
    passInput.focus();
  } catch (err) {
    showFormError(
      form,
      err.name === "AbortError"
        ? "The server took too long to respond. Please try again."
        : "Can't reach the server. Check your connection and try again."
    );
  } finally {
    setLoading(form, false);
  }
});

// Clear a field's error as soon as the user edits it
form.querySelectorAll("input").forEach(function (input) {
  input.addEventListener("input", function () {
    setFieldInvalid(input, false);
    showFormError(form, "");
  });
});