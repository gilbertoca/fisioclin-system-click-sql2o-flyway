package org.apache.click.showcase.fisio.page;

import org.apache.click.Page;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.control.MenuFactory;

public class LayoutPage extends Page {
    private static final long serialVersionUID = 1L;

    // Componente de Menu Nativo do Apache Click Extras
    protected Menu rootMenu;

    public LayoutPage() {
        // Fábrica padrão que lê o arquivo menu.xml do classpath/WEB-INF
        MenuFactory menuFactory = new MenuFactory();
        this.rootMenu = menuFactory.getRootMenu();
        
        // Adiciona o controle na árvore da página mestre
        addControl(rootMenu);
    }

    @Override
    public String getTemplate() {
        return "/layout.htm";
    }
}
