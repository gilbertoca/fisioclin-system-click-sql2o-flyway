package org.apache.click.showcase.fisio.page;

import org.apache.click.Page;
import org.apache.click.element.CssImport;
import org.apache.click.element.JsImport;

import java.util.List;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.control.MenuFactory;

public class LeiautePage extends Page {

    /**
     * The root menu. Note this transient variable is reinitialized in onInit()
     * to support serialized stateful pages.
     */
    private transient Menu rootMenu;

    public LeiautePage() {
    }

    @Override
    public List getHeadElements() {
        if (headElements == null) {
            headElements = super.getHeadElements();

            // Bulma CSS
            headElements.add(new CssImport("/assets/css/bulma.min.css"));

            // CSS customizado
            headElements.add(new CssImport("/assets/css/custom.css"));

            // JS opcional
            headElements.add(new JsImport("/assets/js/custom.js"));
        }
        return headElements;
    }

    /**
     * @see org.apache.click.Page#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();

        // Carrega o menu.xml padrão do WEB-INF
        MenuFactory menuFactory = new MenuFactory();
        rootMenu = menuFactory.getRootMenu();

        // Adiciona classes Bulma no menu raiz
        rootMenu.addStyleClass("menu");
        for (Menu child : rootMenu.getChildren()) {
            child.addStyleClass("menu-list");
        }

        // Adiciona o menu como controle da página
        addControl(rootMenu);
    }

    /**
     * @see org.apache.click.Page#onDestroy()
     */
    @Override
    public void onDestroy() {
        // Remove menu for when BorderPage is serialized
        if (rootMenu != null) {
            removeControl(rootMenu);
        }
    }

    /**
     * @see org.apache.click.Page#getTemplate()
     */
    @Override
    public String getTemplate() {
        return "/leiaute.htm";
    }

}
