
const sidebar = document.getElementById("sidebar");
const menuToggle = document.getElementById("menuToggle");
const toast = document.getElementById("toast");

let toastTimer;
let memberState = null;

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("show");

  clearTimeout(toastTimer);

  toastTimer = setTimeout(() => {
    toast.classList.remove("show");
  }, 2600);
}

const pageNames = {
  overview: "Dashboard",
  profile: "My Profile",
  schedule: "Payment Schedule",
  contributions: "My Contributions",
  history: "Payment History",
  notifications: "Notifications"
};

function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("en-PH", {
    style: "currency",
    currency: "PHP",
    minimumFractionDigits: 2
  }).format(amount);
}

function initialsFromName(name) {
  if (!name) return "MN";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function loadProfilePhoto() {
  document.querySelectorAll(".profile-photo").forEach((image) => {
    const placeholder = image.parentElement.querySelector(".avatar-initials");
    image.onload = () => {
      image.hidden = false;
      if (placeholder) placeholder.hidden = true;
    };
    image.onerror = () => {
      image.hidden = true;
      if (placeholder) placeholder.hidden = false;
    };
    image.src = "/api/profile/photo?v=" + Date.now();
  });
}

async function apiFetch(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: {
      Accept: "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (response.status === 401) {
    window.location.href = "/login&signup/login.html";
    return null;
  }

  if (!response.ok) {
    let errorText = "Request failed.";
    try {
      const errorBody = await response.json();
      errorText = errorBody.message || errorText;
    } catch (_) {
      // ignore
    }
    throw new Error(errorText);
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return response.text();
}

async function updateNotificationCount() {
  try {
    const notifications = await apiFetch("/api/member/notifications/unread");
    if (!Array.isArray(notifications)) return;

    const count = notifications.length;
    const badge = document.querySelector(".nav-link[data-page='notifications'] .nav-count");
    const indicator = document.querySelector(".notification-button i");

    if (badge) {
      badge.textContent = count > 99 ? "99+" : String(count);
      badge.hidden = count === 0;
    }
    if (indicator) indicator.hidden = count === 0;
  } catch (error) {
    console.error("Unable to load notification count.", error);
  }
}

function updateDashboardCards(dashboard) {
  const statCards = document.querySelectorAll(".stat-card");

  if (statCards.length >= 4) {
    statCards[0].querySelector(".stat-value").textContent = formatCurrency(dashboard.totalContributions || 0);
    statCards[1].querySelector(".stat-value").textContent = formatCurrency(dashboard.nextPaymentAmount || 0);
    statCards[2].querySelector(".stat-value").innerHTML = `${dashboard.completedPayments || 0} <small>/ ${(dashboard.completedPayments || 0) + (dashboard.scheduledPayments || 0) || 0}</small>`;
    statCards[3].querySelector(".stat-value").textContent = dashboard.accountStanding || "Not available";

    const duePill = statCards[1].querySelector(".due-pill");
    if (dashboard.nextPaymentDueDate) {
      duePill.textContent = `Due ${new Date(dashboard.nextPaymentDueDate).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric"
      })}`;
    } else {
      duePill.textContent = "No scheduled payment";
    }

    const progress = statCards[2].querySelector(".progress-track span");
    const denominator = (dashboard.completedPayments || 0) + (dashboard.scheduledPayments || 0) || 1;
    const percent = Math.min(100, Math.round(((dashboard.completedPayments || 0) / denominator) * 100));
    progress.style.width = `${percent}%`;
  }

  const contributionTotal = document.querySelector(".contribution-total strong");
  if (contributionTotal) {
    contributionTotal.textContent = formatCurrency(dashboard.totalContributions || 0);
  }

  const activityList = document.querySelector(".activity-list");
  if (activityList && Array.isArray(dashboard.recentActivities)) {
    if (dashboard.recentActivities.length === 0) {
      activityList.innerHTML = '<div class="activity-item" style="justify-content:center; color:#6b7280;">No recent activity.</div>';
      return;
    }

    activityList.innerHTML = dashboard.recentActivities.map((activity) => {
      const formattedDate = activity.paymentDate ? new Date(activity.paymentDate).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric"
      }) : "Recent payment";
      return `
        <div class="activity-item">
          <span class="activity-icon success">✓</span>
          <div class="activity-copy">
            <strong>Contribution received</strong>
            <p>${activity.paymentMethod || "Cash"} · ${formattedDate}</p>
          </div>
          <strong class="activity-amount">+${formatCurrency(activity.amount)}</strong>
        </div>
      `;
    }).join("");
  }

  const upcomingTableBody = document.querySelector(".schedule-panel tbody");
  if (upcomingTableBody && Array.isArray(dashboard.upcomingPayments)) {
    if (dashboard.upcomingPayments.length === 0) {
      upcomingTableBody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#6b7280; padding:1.25rem;">No upcoming payments yet.</td></tr>';
      return;
    }

    upcomingTableBody.innerHTML = dashboard.upcomingPayments.slice(0, 3).map((payment, index) => `
      <tr>
        <td>
          <span class="payment-badge">${String(index + 1).padStart(2, "0")}</span>
          <strong>${payment.description || "Payment"}</strong>
        </td>
        <td>${payment.dueDate ? new Date(payment.dueDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }) : "TBD"}</td>
        <td class="amount">${formatCurrency(payment.amount)}</td>
        <td><span class="pill ${payment.status === "PAID" ? "paid" : payment.status === "OVERDUE" ? "overdue" : "scheduled"}">${payment.status || "Scheduled"}</span></td>
      </tr>
    `).join("");
  }
}

async function loadMemberDashboard() {
  try {
    let user = await apiFetch("/api/auth/me");

    // If backend session expired, check localStorage for cached auth
    if (!user) {
      try {
        const stored = JSON.parse(localStorage.getItem("auth") || "null");
        if (stored && stored.username) {
          user = stored;
        } else {
          window.location.href = "/login&signup/login.html";
          return;
        }
      } catch(_) {
        window.location.href = "/login&signup/login.html";
        return;
      }
    } else {
      // Sync localStorage with the confirmed backend user
      try { localStorage.setItem("auth", JSON.stringify(user)); } catch(_){}
    }

    const dashboard = await apiFetch("/api/member/dashboard");
    if (!dashboard) return;

    memberState = dashboard;

    const fullName = dashboard.memberName || user.username || "Member Name";
    document.querySelector(".user-menu-copy strong").textContent = fullName;
    document.querySelector(".mini-avatar .avatar-initials").textContent = initialsFromName(fullName);
    document.querySelector(".avatar-large .avatar-initials").textContent = initialsFromName(fullName);
    document.querySelector(".profile-main h2").textContent = fullName;
    document.getElementById("pageTitle").innerHTML = `Welcome back, ${fullName.split(" ")[0]} <span>✦</span>`;

    const memberNumber = dashboard.memberNumber || "M-000";
    const memberMeta = document.querySelector(".profile-main p");
    if (memberMeta) {
      memberMeta.innerHTML = `Member ID <strong>#${memberNumber}</strong><span class="bullet">•</span> Active member`;
    }

    loadProfilePhoto();
    updateDashboardCards(dashboard);
    await updateNotificationCount();
  } catch (error) {
    showToast(error.message || "Unable to load dashboard data.");
  }
}

// SIDEBAR NAVIGATION
document.querySelectorAll("[data-page]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".nav-link[data-page]")
      .forEach((item) => item.classList.remove("active"));

    button.classList.add("active");

    const page = button.dataset.page;
    document.getElementById("crumb").textContent = pageNames[page];

    document.getElementById("pageTitle").innerHTML = page === "overview"
      ? `Welcome back, ${memberState?.memberName?.split(" ")[0] || "Member"} <span>✦</span>`
      : pageNames[page];

    if (page === "notifications") {
      openNotifications();
    } else if (page !== "overview") {
      showToast(`${pageNames[page]} selected.`);
    }

    sidebar.classList.remove("open");
  });
});

document.querySelectorAll("[data-goto]").forEach((button) => {
  button.addEventListener("click", () => {
    const page = button.dataset.goto;
    const navItem = document.querySelector(`[data-page="${page}"]`);
    if (navItem) {
      navItem.click();
    }
  });
});

menuToggle.addEventListener("click", () => {
  sidebar.classList.toggle("open");
});

async function openNotifications() {
  const overlay = document.getElementById("notification-overlay");
  const list = document.getElementById("notification-list");
  overlay.hidden = false;
  list.textContent = "Loading notifications...";

  try {
    const notifications = await apiFetch("/api/member/notifications");
    list.replaceChildren();
    if (!Array.isArray(notifications) || notifications.length === 0) {
      list.textContent = "No notifications yet.";
      await updateNotificationCount();
      return;
    }

    notifications.forEach((notification) => {
      const card = document.createElement("article");
      card.className = "notification-card";

      const title = document.createElement("h3");
      title.textContent = notification.title || "Notification";
      card.appendChild(title);

      if (notification.createdDate) {
        const date = document.createElement("time");
        date.dateTime = notification.createdDate;
        date.textContent = new Date(notification.createdDate).toLocaleString();
        card.appendChild(date);
      }

      const message = document.createElement("p");
      message.textContent = notification.message || "";
      card.appendChild(message);

      if (notification.title === "Payment receipt issued") {
        const download = document.createElement("button");
        download.type = "button";
        download.className = "notification-download";
        download.textContent = "Download receipt (.txt)";
        download.addEventListener("click", () => {
          const blob = new Blob([notification.message || ""], { type: "text/plain;charset=utf-8" });
          const url = URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = url;
          link.download = "payment-receipt-" + notification.id + ".txt";
          link.click();
          window.setTimeout(() => URL.revokeObjectURL(url), 1000);
          showToast("Receipt downloaded.");
        });
        card.appendChild(download);
      }

      list.appendChild(card);
    });

    try {
      await apiFetch("/api/member/notifications/read-all", { method: "PATCH" });
      await updateNotificationCount();
    } catch (error) {
      console.error("Unable to mark notifications as read.", error);
    }
  } catch (error) {
    list.textContent = error.message || "Unable to load notifications.";
  }
}

function closeNotifications() {
  document.getElementById("notification-overlay").hidden = true;
}

document.getElementById("notification-close").addEventListener("click", closeNotifications);
document.getElementById("notification-overlay").addEventListener("click", (event) => {
  if (event.target.id === "notification-overlay") closeNotifications();
});

const profilePhotoInput = document.getElementById("profile-photo-input");
document.getElementById("change-profile-photo").addEventListener("click", () => profilePhotoInput.click());
profilePhotoInput.addEventListener("change", async () => {
  const file = profilePhotoInput.files[0];
  profilePhotoInput.value = "";
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);
  try {
    await apiFetch("/api/profile/photo", { method: "POST", body: formData });
    loadProfilePhoto();
    showToast("Profile photo updated.");
  } catch (error) {
    showToast(error.message || "Unable to upload profile photo.");
  }
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") closeNotifications();
});

document.getElementById("downloadBtn").addEventListener("click", () => {
  const statement = [
    "MEMBERHUB — MEMBER STATEMENT",
    `Member: ${memberState?.memberName || "Member Name"}`,
    `Member ID: ${memberState?.memberNumber || "M-000"}`,
    `Total contributions: ${formatCurrency(memberState?.totalContributions || 0)}`,
    `Next payment: ${formatCurrency(memberState?.nextPaymentAmount || 0)}`,
    "",
    "Generated from system records."
  ].join("\n");

  const blob = new Blob([statement], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = "member-statement.txt";
  link.click();
  URL.revokeObjectURL(url);

  showToast("Statement downloaded.");
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    showToast("Logging out...");
    // Clear local auth cache
    try { localStorage.removeItem("auth"); } catch(_){}
    await fetch("/api/auth/logout", {
      method: "POST",
      credentials: "same-origin"
    });
    window.location.href = "/login&signup/login.html";
  } catch (error) {
    try { localStorage.removeItem("auth"); } catch(_){}
    window.location.href = "/login&signup/login.html";
  }
});

document.getElementById("userMenu").addEventListener("click", () => {
  showToast("Member account selected.");
});

window.setInterval(updateNotificationCount, 30000);
window.addEventListener("focus", updateNotificationCount);

loadMemberDashboard();
