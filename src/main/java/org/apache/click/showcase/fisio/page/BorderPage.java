package org.apache.click.showcase.fisio.page;

import org.apache.click.Page;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.control.MenuFactory;

/**
 * Border Template Page com menu dinâmico (Login/Sair).
 */
public class BorderPage extends Page {

    private static final long serialVersionUID = 1L;
    private transient Menu rootMenu;

    @Override
    public void onInit() {
        super.onInit();

        MenuFactory factory = new MenuFactory();
        rootMenu = factory.getRootMenu();

        // === MENU DINÂMICO: Login / Sair ===
        boolean isLoggedIn = getContext().getSessionAttribute("user") != null;

        Menu authMenu = new Menu(isLoggedIn ? "Sair" : "Login");
        authMenu.setPath(isLoggedIn ? "login.htm?action=logout" : "login.htm");
        authMenu.setImageSrc("/assets/images/user.png"); // opcional
        rootMenu.add(authMenu);

        addControl(rootMenu);
    }

    @Override
    public void onDestroy() {
        if (rootMenu != null) {
            removeControl(rootMenu);
        }
    }

    @Override
    public String getTemplate() {
        return "/border-template.htm";
    }
}