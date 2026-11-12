
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
    const user = await apiFetch("/api/auth/me");
    if (!user) return;

    const dashboard = await apiFetch("/api/member/dashboard");
    if (!dashboard) return;

    memberState = dashboard;

    const fullName = dashboard.memberName || user.username || "Member Name";
    document.querySelector(".user-menu-copy strong").textContent = fullName;
    document.querySelector(".mini-avatar").textContent = initialsFromName(fullName);
    document.querySelector(".avatar-large").textContent = initialsFromName(fullName);
    document.querySelector(".profile-main h2").textContent = fullName;
    document.getElementById("pageTitle").innerHTML = `Welcome back, ${fullName.split(" ")[0]} <span>✦</span>`;

    const memberNumber = dashboard.memberNumber || "M-000";
    const memberMeta = document.querySelector(".profile-main p");
    if (memberMeta) {
      memberMeta.innerHTML = `Member ID <strong>#${memberNumber}</strong><span class="bullet">•</span> Active member`;
    }

    updateDashboardCards(dashboard);
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

    if (page !== "overview") {
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
    await fetch("/api/auth/logout", {
      method: "POST",
      credentials: "same-origin"
    });
    window.location.href = "/login&signup/login.html";
  } catch (error) {
    window.location.href = "/login&signup/login.html";
  }
});

document.getElementById("userMenu").addEventListener("click", () => {
  showToast("Member account selected.");
});

loadMemberDashboard();
