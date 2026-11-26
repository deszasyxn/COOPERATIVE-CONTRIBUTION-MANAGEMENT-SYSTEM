// ---- API helper --------------------------------------------
function api(url, options) {
  options = options || {};
  options.credentials = "same-origin";
  options.headers = options.headers || {};
  if (options.body && typeof options.body === "object" && !(options.body instanceof FormData)) {
    if (!options.headers["Content-Type"]) options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(options.body);
  }
  return fetch(url, options).then(function (res) {
    if (res.status === 401) { window.location.href = "/login&signup/login.html"; return Promise.reject("unauthorized"); }
    if (!res.ok && res.status !== 204) {
      return res.json().then(
        function (err) { throw err; },
        function () { throw { message: "Request failed (" + res.status + ")" }; }
      );
    }
    if (res.status === 204) return Promise.resolve(null);
    return res.json();
  });
}

// ---- Sidebar toggle (tablet / phone) -----------------------
const sidebar = document.getElementById("sidebar");
const scrim = document.getElementById("scrim");
const menuBtn = document.getElementById("menu-btn");

function setSidebar(open) {
  sidebar.classList.toggle("open", open);
  scrim.hidden = !open;
  menuBtn.setAttribute("aria-expanded", String(open));
}

menuBtn.addEventListener("click", function () {
  setSidebar(!sidebar.classList.contains("open"));
});
scrim.addEventListener("click", function () { setSidebar(false); });

// ---- Profile dropdown --------------------------------------
const profileBtn = document.getElementById("profile-btn");
const profileMenu = document.getElementById("profile-menu");

function setProfileMenu(open) {
  profileMenu.hidden = !open;
  profileBtn.setAttribute("aria-expanded", String(open));
}

profileBtn.addEventListener("click", function (e) {
  e.stopPropagation();
  setProfileMenu(profileMenu.hidden);
});
document.addEventListener("click", function (e) {
  if (!profileMenu.contains(e.target)) setProfileMenu(false);
});
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape") {
    setProfileMenu(false);
    setSidebar(false);
    closeAllModals();
  }
});

// ---- Highlight the sidebar link for the section in view ----
const navLinks = Array.from(document.querySelectorAll(".nav-link[href^='#']"));

function setActive(id) {
  navLinks.forEach(function (link) {
    const isActive = link.getAttribute("href") === "#" + id;
    link.classList.toggle("active", isActive);
    if (isActive) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
}

navLinks.forEach(function (link) {
  link.addEventListener("click", function (e) {
    var href = link.getAttribute("href");
    if (href === "#") {
      e.preventDefault();
      return;
    }
    setActive(href.slice(1));
    setSidebar(false);
  });
});

const sections = navLinks
  .filter(function (l) { var href = l.getAttribute("href"); return href && href.length > 1; })
  .map(function (l) { return document.querySelector(l.getAttribute("href")); })
  .filter(Boolean);

if ("IntersectionObserver" in window) {
  const observer = new IntersectionObserver(
    function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) setActive(entry.target.id);
      });
    },
    { rootMargin: "-25% 0px -60% 0px" }
  );
  sections.forEach(function (s) { observer.observe(s); });
}

// ---- Modal helpers -----------------------------------------
var modalOverlay = document.getElementById("modal-overlay");

function openModal(id) {
  modalOverlay.hidden = false;
  document.getElementById(id).hidden = false;
}

function closeModal(id) {
  document.getElementById(id).hidden = true;
  var anyOpen = document.querySelector(".modal:not([hidden])");
  if (!anyOpen) modalOverlay.hidden = true;
}

function closeAllModals() {
  document.querySelectorAll(".modal").forEach(function (m) { m.hidden = true; });
  modalOverlay.hidden = true;
}

document.addEventListener("click", function (e) {
  var closeBtn = e.target.closest("[data-modal]");
  if (closeBtn) closeModal(closeBtn.getAttribute("data-modal"));
  var closeBtn2 = e.target.closest("[data-modal-close]");
  if (closeBtn2) closeModal(closeBtn2.getAttribute("data-modal-close"));
});
modalOverlay.addEventListener("click", closeAllModals);

// ---- Status badge helper -----------------------------------
function statusBadge(status) {
  var cls = "status-" + status.toLowerCase();
  return '<span class="status-badge ' + cls + '">' + status + '</span>';
}

// ---- Cached data -------------------------------------------
var membersCache = [];

// ---- Load overview stats -----------------------------------
function loadStats() {
  api("/api/admin/reports").then(function (data) {
    document.querySelector('[data-stat="members"]').textContent = data.totalMembers || 0;
    document.querySelector('[data-stat="upcoming"]').textContent = data.upcomingPayments || 0;
    var contrib = data.totalContributions || 0;
    document.querySelector('[data-stat="contributions"]').textContent = "\u20B1" + Number(contrib).toLocaleString("en-PH", {minimumFractionDigits: 2, maximumFractionDigits: 2});
    var now = new Date();
    var updated = document.querySelector('[data-stat="last-updated"]');
    if (updated) updated.textContent = now.toLocaleDateString("en-PH", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
  }).catch(function () {
    // Stats remain at defaults
  });
}

// ---- Load members ------------------------------------------
function loadMembers() {
  return api("/api/admin/members").then(function (data) {
    membersCache = data || [];
    renderMembersTable(membersCache);
    populateMemberSelect(membersCache);
  });
}

function renderMembersTable(members) {
  var tbody = document.getElementById("members-tbody");
  if (!members || members.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="loading-text">No members found.</td></tr>';
    return;
  }
  var html = "";
  members.forEach(function (m) {
    html += '<tr>' +
      '<td>' + escapeHtml(m.memberNumber || "\u2014") + '</td>' +
      '<td>' + escapeHtml(m.name) + '</td>' +
      '<td>' + escapeHtml(m.email) + '</td>' +
      '<td>' + escapeHtml(m.phone || "\u2014") + '</td>' +
      '<td>' + statusBadge(m.membershipStatus) + '</td>' +
      '<td>' +
        '<button class="table-action-btn" onclick="editMember(' + m.id + ')">Edit</button> ' +
        '<button class="table-action-btn danger" onclick="deactivateMember(' + m.id + ',\'' + escapeAttr(m.name) + '\')">Deactivate</button>' +
      '</td>' +
    '</tr>';
  });
  tbody.innerHTML = html;
}

function populateMemberSelect(members) {
  var sel = document.getElementById("payment-member");
  if (!sel) return;
  var html = '<option value="">Select a member</option>';
  members.forEach(function (m) {
    html += '<option value="' + m.id + '">' + escapeHtml(m.name) + " (" + escapeHtml(m.memberNumber || "") + ")</option>";
  });
  sel.innerHTML = html;
}

// ---- Member form -------------------------------------------
function showAddMemberForm() {
  document.getElementById("member-form-title").textContent = "Add Member";
  document.getElementById("member-form-id").value = "";
  document.getElementById("member-form-name").value = "";
  document.getElementById("member-form-email").value = "";
  document.getElementById("member-form-username").value = "";
  document.getElementById("member-form-password").value = "";
  document.getElementById("member-form-password").required = true;
  setMemberPasswordVisibility(false);
  document.getElementById("member-form-phone").value = "";
  document.getElementById("member-form-address").value = "";
  document.getElementById("member-form-status").value = "ACTIVE";
  document.getElementById("member-form-error").textContent = "";
  openModal("modal-member-form");
}

function editMember(id) {
  var m = membersCache.find(function (mem) { return mem.id === id; });
  if (!m) return;
  document.getElementById("member-form-title").textContent = "Edit Member";
  document.getElementById("member-form-id").value = id;
  document.getElementById("member-form-name").value = m.name || "";
  document.getElementById("member-form-email").value = m.email || "";
  document.getElementById("member-form-username").value = "";
  document.getElementById("member-form-password").value = "";
  document.getElementById("member-form-password").required = false;
  setMemberPasswordVisibility(false);
  document.getElementById("member-form-phone").value = m.phone || "";
  document.getElementById("member-form-address").value = m.address || "";
  document.getElementById("member-form-status").value = m.membershipStatus || "ACTIVE";
  document.getElementById("member-form-error").textContent = "";
  openModal("modal-member-form");
}

function setMemberPasswordVisibility(isVisible) {
  var passwordInput = document.getElementById("member-form-password");
  var toggle = document.getElementById("member-password-toggle");
  passwordInput.type = isVisible ? "text" : "password";
  toggle.textContent = isVisible ? "Hide" : "Show";
  toggle.setAttribute("aria-pressed", String(isVisible));
}

document.getElementById("member-password-toggle").addEventListener("click", function () {
  var passwordInput = document.getElementById("member-form-password");
  setMemberPasswordVisibility(passwordInput.type === "password");
});

function submitMemberForm(e) {
  e.preventDefault();
  var errEl = document.getElementById("member-form-error");
  errEl.textContent = "";

  var id = document.getElementById("member-form-id").value;
  var name = document.getElementById("member-form-name").value.trim();
  var email = document.getElementById("member-form-email").value.trim();
  var username = document.getElementById("member-form-username").value.trim();
  var password = document.getElementById("member-form-password").value;
  var phone = document.getElementById("member-form-phone").value.trim();
  var address = document.getElementById("member-form-address").value.trim();
  var status = document.getElementById("member-form-status").value;

  if (!name || !email) { errEl.textContent = "Name and email are required."; return; }

  var isEdit = !!id;
  if (!isEdit && !username) { errEl.textContent = "Username is required for new members."; return; }
  if (!isEdit && !password) { errEl.textContent = "Password is required for new members."; return; }

  var body = { name: name, email: email, membershipStatus: status };
  if (phone) body.phone = phone;
  if (address) body.address = address;
  if (!isEdit) { body.username = username; body.password = password; }

  var url = isEdit ? "/api/admin/members/" + id : "/api/admin/members";
  var method = isEdit ? "PUT" : "POST";

  api(url, { method: method, body: body }).then(function () {
    closeModal("modal-member-form");
    loadMembers();
    loadStats();
  }).catch(function (err) {
    errEl.textContent = err.message || "Failed to save member.";
  });
}

function deactivateMember(id, name) {
  if (!confirm("Are you sure you want to deactivate " + name + "?")) return;
  api("/api/admin/members/" + id + "/status", {
    method: "PATCH",
    body: { membershipStatus: "INACTIVE" }
  }).then(function () {
    loadMembers();
    loadStats();
  }).catch(function (err) {
    alert("Failed to deactivate member: " + (err.message || "Unknown error"));
  });
}

// ---- Payment form ------------------------------------------
function showPaymentForm() {
  document.getElementById("payment-member").value = "";
  document.getElementById("payment-reference").value = "";
  document.getElementById("payment-description").value = "";
  document.getElementById("payment-amount").value = "";
  document.getElementById("payment-due-date").value = new Date().toISOString().slice(0, 10);
  document.getElementById("payment-status").value = "SCHEDULED";
  document.getElementById("payment-form-error").textContent = "";
  openModal("modal-payment");
}

function submitPaymentForm(e) {
  e.preventDefault();
  var errEl = document.getElementById("payment-form-error");
  errEl.textContent = "";

  var memberId = document.getElementById("payment-member").value;
  var referenceNumber = document.getElementById("payment-reference").value.trim();
  var description = document.getElementById("payment-description").value.trim();
  var amount = document.getElementById("payment-amount").value;
  var dueDate = document.getElementById("payment-due-date").value;
  var status = document.getElementById("payment-status").value;

  if (!memberId || !referenceNumber || !description || !amount || !dueDate) {
    errEl.textContent = "All required fields must be filled.";
    return;
  }

  api("/api/admin/payments", {
    method: "POST",
    body: {
      memberId: Number(memberId),
      referenceNumber: referenceNumber,
      description: description,
      amount: parseFloat(amount),
      dueDate: dueDate,
      status: status
    }
  }).then(function () {
    closeModal("modal-payment");
    loadStats();
  }).catch(function (err) {
    errEl.textContent = err.message || "Failed to record payment.";
  });
}

// ---- Payment history ---------------------------------------
function loadPaymentHistory() {
  openModal("modal-payment-history");
  var tbody = document.getElementById("payment-history-tbody");
  tbody.innerHTML = '<tr><td colspan="6" class="loading-text">Loading payments...</td></tr>';

  // Fetch all payments from admin endpoint
  api("/api/admin/payments").then(function (data) {
    if (!data || data.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="loading-text">No payments found.</td></tr>';
      return;
    }
    var html = "";
    data.forEach(function (p) {
      html += '<tr>' +
        '<td>' + escapeHtml(p.referenceNumber || "\u2014") + '</td>' +
        '<td>' + escapeHtml(p.description || "\u2014") + '</td>' +
        '<td>\u20B1' + Number(p.amount || 0).toLocaleString("en-PH", {minimumFractionDigits: 2}) + '</td>' +
        '<td>' + escapeHtml(p.dueDate || "\u2014") + '</td>' +
        '<td>' + statusBadge(p.status) + '</td>' +
        '<td>' +
          (p.status === "PAID" ? '<button class="table-action-btn" onclick="issueReceipt(' + p.id + ')">Issue receipt</button>' : '<button class="table-action-btn" onclick="confirmPayment(' + p.id + ')">Confirm</button>') +
        '</td>' +
      '</tr>';
    });
    tbody.innerHTML = html;
  }).catch(function () {
    tbody.innerHTML = '<tr><td colspan="6" class="loading-text">Failed to load payments.</td></tr>';
  });
}

function confirmPayment(id) {
  api("/api/admin/payments/" + id + "/confirm", { method: "POST" }).then(function () {
    loadPaymentHistory();
    loadStats();
  }).catch(function (err) {
    alert("Failed to confirm payment: " + (err.message || "Unknown error"));
  });
}

function issueReceipt(id) {
  fetch("/api/admin/payments/" + id + "/receipt", {
    method: "POST",
    credentials: "same-origin",
    headers: { Accept: "text/plain" }
  }).then(function (response) {
    if (response.status === 401) {
      window.location.href = "/login&signup/login.html";
      throw new Error("Your session has expired.");
    }
    if (!response.ok) {
      return response.json().then(function (error) {
        throw new Error(error.message || "Failed to issue receipt.");
      }, function () {
        throw new Error("Failed to issue receipt.");
      });
    }
    return response.blob();
  }).then(function (blob) {
    var url = URL.createObjectURL(blob);
    var link = document.createElement("a");
    link.href = url;
    link.download = "payment-receipt-" + id + ".txt";
    link.click();
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    alert("Receipt downloaded and sent to the member's account.");
  }).catch(function (err) {
    alert(err.message || "Failed to issue receipt.");
  });
}

// ---- Logout ------------------------------------------------
function logout() {
  // Clear local auth cache
  try { localStorage.removeItem("auth"); } catch(_){}
  api("/api/auth/logout", { method: "POST" }).then(function () {
    window.location.href = "/login&signup/login.html";
  }).catch(function () {
    window.location.href = "/login&signup/login.html";
  });
}

// ---- Escape helpers ----------------------------------------
function escapeHtml(str) {
  var div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

function escapeAttr(str) {
  return str.replace(/'/g, "\\'").replace(/"/g, "&quot;");
}

// ---- Wire up action buttons in cards -----------------------
document.addEventListener("click", function (e) {
  var action = e.target.closest(".action");
  if (!action) return;
  var text = action.textContent.trim();

  // Member Records actions
  if (text === "View Members" || text === "Add Member") {
    if (text === "View Members") openModal("modal-members");
    else showAddMemberForm();
    e.preventDefault();
  }
  if (text === "Edit Member") {
    openModal("modal-members");
    e.preventDefault();
  }
  if (text === "Deactivate Member") {
    openModal("modal-members");
    e.preventDefault();
  }

  // Payment Schedule actions
  if (text === "Set Payment Schedule" || text === "Set Due Dates") {
    showPaymentForm();
    e.preventDefault();
  }
  if (text.indexOf("Upcoming") >= 0 || text.indexOf("Overdue") >= 0) {
    loadPaymentHistory();
    e.preventDefault();
  }

  // Payment Computation actions
  if (text.indexOf("Compute") >= 0 || text.indexOf("Calculate") >= 0) {
    loadPaymentHistory();
    e.preventDefault();
  }

  // Payment Records actions
  if (text === "Record Payment") {
    showPaymentForm();
    e.preventDefault();
  }
  if (text === "Update Payment Status") {
    loadPaymentHistory();
    e.preventDefault();
  }
  if (text.indexOf("Receipt") >= 0) {
    loadPaymentHistory();
    e.preventDefault();
  }
  if (text === "View Payment History") {
    loadPaymentHistory();
    e.preventDefault();
  }
  if (text === "Generate Reports") {
    loadStats();
    alert("Reports: Total Members: " + (document.querySelector('[data-stat="members"]').textContent) +
          "\nUpcoming Payments: " + (document.querySelector('[data-stat="upcoming"]').textContent) +
          "\nTotal Contributions: " + (document.querySelector('[data-stat="contributions"]').textContent));
    e.preventDefault();
  }

  // System Logs
  if (text === "View logs") {
    alert("System logs feature coming soon.");
    e.preventDefault();
  }
});

// ---- Wire up modal buttons ---------------------------------
document.getElementById("btn-add-member").addEventListener("click", showAddMemberForm);
document.getElementById("member-form").addEventListener("submit", submitMemberForm);
document.getElementById("payment-form").addEventListener("submit", submitPaymentForm);

// ---- Profile picture ---------------------------------------
var profilePhotoInput = document.getElementById("profile-photo-input");
var profilePhoto = document.querySelector(".avatar .profile-photo");
var profilePlaceholder = document.querySelector(".avatar-placeholder");

function loadProfilePhoto() {
  profilePhoto.onload = function () {
    profilePhoto.hidden = false;
    profilePlaceholder.hidden = true;
  };
  profilePhoto.onerror = function () {
    profilePhoto.hidden = true;
    profilePlaceholder.hidden = false;
  };
  profilePhoto.src = "/api/profile/photo?v=" + Date.now();
}

document.getElementById("change-profile-photo").addEventListener("click", function () {
  setProfileMenu(false);
  profilePhotoInput.click();
});

profilePhotoInput.addEventListener("change", function () {
  var file = profilePhotoInput.files[0];
  profilePhotoInput.value = "";
  if (!file) return;

  var formData = new FormData();
  formData.append("file", file);
  api("/api/profile/photo", { method: "POST", body: formData })
    .then(function () {
      loadProfilePhoto();
      alert("Profile photo updated.");
    })
    .catch(function (error) {
      alert(error.message || "Unable to upload profile photo.");
    });
});

// ---- Wire up logout buttons --------------------------------
document.querySelectorAll(".logout").forEach(function (btn) {
  btn.addEventListener("click", function (e) {
    e.preventDefault();
    logout();
  });
});

// Profile menu logout
document.getElementById("profile-logout").addEventListener("click", function (e) {
  e.preventDefault();
  setProfileMenu(false);
  logout();
});

// ---- Initialize dashboard ----------------------------------
(function init() {
  // Check auth state: verify with backend, fall back to localStorage
  api("/api/auth/me")
    .then(function(user) {
      // Sync localStorage with the confirmed backend user
      try { localStorage.setItem("auth", JSON.stringify(user)); } catch(_){}
      // Display the logged-in user's name
      var nameEl = document.querySelector(".profile-name");
      if (nameEl && user.username) nameEl.textContent = user.username;
      var welcomeH1 = document.querySelector(".welcome h1");
      if (welcomeH1 && user.username) welcomeH1.textContent = "Welcome, " + user.username + "!";
      loadProfilePhoto();
      // Dashboard is ready — load data
      loadStats();
      loadMembers();
    })
    .catch(function() {
      // Backend says not authenticated — check localStorage as a hint
      var stored = null;
      try { stored = JSON.parse(localStorage.getItem("auth") || "null"); } catch(_){}
      if (!stored || !stored.username) {
        // No local hint either — send to login
        window.location.href = "/login&signup/login.html";
        return;
      }
      // We have a local hint but the session expired — show the user's name
      // from cache and let the dashboard work with cached data until logout
      var nameEl = document.querySelector(".profile-name");
      if (nameEl) nameEl.textContent = stored.username;
      var welcomeH1 = document.querySelector(".welcome h1");
      if (welcomeH1) welcomeH1.textContent = "Welcome, " + stored.username + "!";
      // Try to reload data; if API calls return 401, the api() helper redirects
      loadStats();
      loadMembers();
    });
})();