package com.gilbertoca.fisio.page;

import org.apache.click.Context;
import org.apache.click.Page;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.control.MenuFactory;

/**
 * Base layout page. Every page that needs the shared border template
 * should extend this class.
 *
 * The root menu is loaded from /WEB-INF/menu.xml via MenuFactory and
 * registered as a page control so its head elements are deployed.
 *
 * Two model values are exposed to every template:
 *   - $authenticated : Boolean, true if a user is logged in
 *   - $title         : String, default page title (pages may override)
 *
 * Both are set in onInit(), which is the correct lifecycle phase for
 * request-dependent initialization.
 */
public abstract class BorderPage extends Page {

    protected Menu rootMenu;

    public BorderPage() {
        MenuFactory menuFactory = new MenuFactory();
        rootMenu = menuFactory.getRootMenu();
    }

    @Override
    public void onInit() {
        super.onInit();

        Context ctx = getContext();
        boolean authenticated = ctx.hasSession()
                && ctx.getSessionAttribute(AuthenticatedPage.SESSION_USER) != null;
        addModel("authenticated", Boolean.valueOf(authenticated));

        Menu authMenu = new Menu(authenticated ? "Log out" : "Log in");
        authMenu.setPath(authenticated ? "login.htm?action=logout" : "login.htm");
        authMenu.setImageSrc("/assets/images/user.png"); // opcional
        rootMenu.add(authMenu);
        addControl(rootMenu);
        
        // Default page title. Pages can override by calling
        // addModel("title", "...") in their own onInit().
        if (getModel().get("title") == null) {
            addModel("title", "Click + Sql2o");
        }
    }

    @Override
    public String getTemplate() {
        return "/WEB-INF/velocity/border-template.htm";
    }
}
