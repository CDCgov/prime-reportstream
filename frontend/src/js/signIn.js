
let signIn = document.getElementById("signIn");
let navMenu = document.getElementById("navmenu");
if (window.sessionStorage.getItem("jwt")) {
    if (signIn) signIn.style.display = "none";
} else {
    if (signIn) signIn.style.display = "block";
    navMenu.style.display = "none";
}
