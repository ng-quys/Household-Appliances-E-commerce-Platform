document.addEventListener("DOMContentLoaded", function () {
    const toggleButton = document.getElementById("adminSidebarToggle");
    const body = document.body;

    if (!toggleButton) return;

    const savedState = localStorage.getItem("adminSidebarCollapsed");

    if (savedState === "true") {
        body.classList.add("admin-sidebar-collapsed");
    }

    toggleButton.addEventListener("click", function () {
        body.classList.toggle("admin-sidebar-collapsed");

        const isCollapsed = body.classList.contains("admin-sidebar-collapsed");
        localStorage.setItem("adminSidebarCollapsed", isCollapsed);
    });
});