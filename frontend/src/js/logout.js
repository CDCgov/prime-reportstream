
  const logout = () => {
    window.sessionStorage.removeItem( "jwt" )
    window.sessionStorage.removeItem( "idle-timer" )
    window.location.replace(`${window.location.origin}`);
      let signIn = document.getElementById("signIn");
    if( signIn )
      signIn.style.display = "block";
  }
