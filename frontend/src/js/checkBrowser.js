const browser = bowser.getParser(window.navigator.userAgent);
const isValidBrowser = browser.satisfies({
    "edge": ">86.0",
    "chrome": ">86.0",
    "firefox": ">78.0",
    "safari": ">14"
});


if( !isValidBrowser )
  window.location.replace( 'unsupported-browser.html' );
