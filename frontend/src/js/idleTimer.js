  let loggedIn = window.sessionStorage.getItem("jwt");

  if( loggedIn ){    
      console.log( 'started idle timer' );
      window.sessionStorage.setItem("idle-timer","true");
      const instance = idleTimeout( () => {
          window.sessionStorage.clear()
          window.location.replace(`/sign-in/`);
      },
      {
          element: document,
          timeout: 1000 * 60 * 15,
          loop: false
      });
  }
