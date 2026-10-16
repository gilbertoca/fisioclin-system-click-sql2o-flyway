package com.gilbertoca.fisio.page;

/**
 * Logs the user out, clears the session, and redirects to the home page.
 */
public class LogoutPage extends BorderPage {

    @Override
    public void onInit() {
        super.onInit();
        getContext().getSession().invalidate();
        setRedirect(HomePage.class);
    }
}
