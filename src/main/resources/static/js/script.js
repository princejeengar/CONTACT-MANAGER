console.log("this is script file");

const toggleSidebar = () => {
    console.log("Toggling sidebar");
    if ($(".sidebar").is(":visible")) {
        console.log("Sidebar is visible, hiding sidebar");
        $(".sidebar").css("display", "none");
        $(".content").css("margin-left", "0%");
    } else {
        console.log("Sidebar is hidden, showing sidebar");
        $(".sidebar").css("display", "block");
        $(".content").css("margin-left", "20%");
    }
}