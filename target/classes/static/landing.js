// Sidebar toggle (mobile / tablet)
var sidebar = document.getElementById("sidebar");
var scrim = document.getElementById("scrim");
var menuBtn = document.getElementById("menu-btn");

function setSidebar(open) {
  sidebar.classList.toggle("open", open);
  scrim.hidden = !open;
  menuBtn.setAttribute("aria-expanded", String(open));
}

menuBtn.addEventListener("click", function () {
  setSidebar(!sidebar.classList.contains("open"));
});
scrim.addEventListener("click", function () { setSidebar(false); });
document.addEventListener("keydown", function (e) {
  if (e.key === "Escape") setSidebar(false);
});

// Fill the overview values from your backend later, e.g.:
// document.querySelector('[data-stat="members"]').textContent = data.totalMembers;
// document.querySelector('[data-stat="upcoming"]').textContent = data.upcomingPayments;
// document.querySelector('[data-stat="contributions"]').textContent = "₱" + data.totalContributions.toLocaleString();
// document.querySelector('[data-stat="last-updated"]').textContent = data.lastUpdated;