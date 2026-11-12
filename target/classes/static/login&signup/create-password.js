const form = document.getElementById("password-form");
const passwordInput = form.elements["password"];
const confirmPasswordInput = form.elements["confirmPassword"];
const submitButton = document.getElementById("create-account-btn");
const feedback = document.getElementById("setup-feedback");
const setupToken = window.location.hash.slice(1);

document.querySelectorAll(".toggle-pw").forEach(function (button) {
  button.addEventListener("click", function () {
    const input = document.getElementById(button.getAttribute("aria-controls"));
    const showing = input.type === "text";
    input.type = showing ? "password" : "text";
    button.classList.toggle("showing", !showing);
    button.setAttribute("aria-label", showing ? "Show password" : "Hide password");
  });
});

function showFeedback(message, success = false) {
  feedback.textContent = message;
  feedback.classList.toggle("success", success);
  feedback.setAttribute("role", success ? "status" : "alert");
  feedback.hidden = !message;
}

if (setupToken) {
  window.history.replaceState(null, "", window.location.pathname);
} else {
  showFeedback("This account setup link is invalid or has expired.");
  submitButton.disabled = true;
}

form.addEventListener("submit", async function (event) {
  event.preventDefault();
  showFeedback("");

  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  if (new TextEncoder().encode(password).length > 72) {
    showFeedback("Password must not exceed 72 bytes.");
    return;
  }
  if (password.length < 8) {
    showFeedback("Password must be at least 8 characters.");
    return;
  }
  if (password !== confirmPassword) {
    showFeedback("Passwords do not match.");
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = "CREATING ACCOUNT...";

  try {
    const response = await fetch("/api/signup/complete", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify({
        token: setupToken,
        password: password,
        confirmPassword: confirmPassword
      })
    });

    let data = {};
    try { data = await response.json(); } catch (_) { /* response had no JSON body */ }

    if (!response.ok) {
      showFeedback(data.message || "Unable to create the account. Request a new signup link and try again.");
      return;
    }

    if (!data.redirect) {
      showFeedback("Account created, but the dashboard redirect was not returned.");
      return;
    }

    window.location.assign(data.redirect);
  } catch (_) {
    showFeedback("Can't reach the server. Check your connection and try again.");
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "CREATE ACCOUNT";
  }
});
