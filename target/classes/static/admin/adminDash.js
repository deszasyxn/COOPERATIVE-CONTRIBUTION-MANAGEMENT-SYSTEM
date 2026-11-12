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

// Clicking a link highlights it right away and closes the mobile menu
navLinks.forEach(function (link) {
  link.addEventListener("click", function () {
    setActive(link.getAttribute("href").slice(1));
    setSidebar(false);
  });
});

// Scrolling updates the highlight
const sections = navLinks
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

// ---- Fill the overview values from your backend later ------
// document.querySelector('[data-stat="members"]').textContent = data.totalMembers;
// document.querySelector('[data-stat="upcoming"]').textContent = data.upcomingPayments;
// document.querySelector('[data-stat="contributions"]').textContent = "₱" + data.totalContributions.toLocaleString();
// document.querySelector('[data-stat="last-updated"]').textContent = data.lastUpdated;