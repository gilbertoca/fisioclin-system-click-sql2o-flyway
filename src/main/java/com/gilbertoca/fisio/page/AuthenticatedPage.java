package com.gilbertoca.fisio.page;

import org.apache.click.Context;

/**
 * Base class for pages that require authentication.
 *
 * onSecurityCheck() is used purely as a gate: it returns true to let
 * processing continue, or sets a redirect and returns false.
 */
public abstract class AuthenticatedPage extends BorderPage {

    public static final String SESSION_USER = "authenticatedUser";

    @Override
    public boolean onSecurityCheck() {
        Context ctx = getContext();
        if (ctx.hasSession() && ctx.getSessionAttribute(SESSION_USER) != null) {
            return true;
        }
        setRedirect(LoginPage.class);
        return false;
    }

    public static boolean isAuthenticated(Context context) {
        return context.hasSession()
                && context.getSessionAttribute(SESSION_USER) != null;
    }
}
