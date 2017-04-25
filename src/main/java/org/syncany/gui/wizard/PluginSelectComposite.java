package org.syncany.gui.wizard;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.syncany.api.transfer.Plugin;
import org.syncany.api.transfer.TransferPlugin;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.util.WidgetDecorator;
import org.syncany.plugins.Plugins;
import org.syncany.util.EnvironmentUtil;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class PluginSelectComposite extends Composite {
	private static final String PLUGIN_ICON_RESOURCE_FORMAT = "/" + Plugin.class.getPackage().getName().replace('.', '/') + "/%s/icon24.png";
	
	private Table pluginTable;
	private List<TransferPlugin> plugins;
	private TransferPlugin selectedPlugin;
	
	public PluginSelectComposite(Composite parent, int style) {
		super(parent, style);	
		
		this.plugins = Plugins.transferPlugins();
		this.selectedPlugin = null;
		
		this.createControls();
	}
				
	private void createControls() {		
		// Main composite
		GridLayout mainCompositeGridLayout = new GridLayout(1, true);
		mainCompositeGridLayout.marginTop = 0;
		mainCompositeGridLayout.marginLeft = 0;
		mainCompositeGridLayout.marginRight = 0;
		mainCompositeGridLayout.marginBottom = 0;
		mainCompositeGridLayout.horizontalSpacing = 0;
		mainCompositeGridLayout.verticalSpacing = 0;

		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));		
		setLayout(mainCompositeGridLayout);		
		
		// Plugin list
		GridData pluginTableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		pluginTableGridData.verticalIndent = 0;
		pluginTableGridData.horizontalIndent = 0;
		
	    pluginTable = new Table(this, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		pluginTable.setHeaderVisible(false);
		pluginTable.setLayoutData(pluginTableGridData);
		
		if (EnvironmentUtil.isWindows()) {
			pluginTable.setBackground(WidgetDecorator.WHITE);
		}
		
		pluginTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Set selected plugin
				if (pluginTable.getSelectionIndex() >= 0) {
					TableItem tableItem = pluginTable.getItem(pluginTable.getSelectionIndex());
					selectedPlugin = (TransferPlugin) tableItem.getData();
				}
				else {
					selectedPlugin = null;
				}
				
				// Fix flickering images
				pluginTable.redraw();
			}
		});	
		
		pluginTable.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				PluginSelectComposite.this.notifyListeners(SWT.FocusIn, new Event());
			}
		});
		
		pluginTable.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {				
				event.height = 30; // Row height workaround
			}
		});		
		
		pluginTable.getVerticalBar().addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				pluginTable.redraw(); // Fix flickering images (when scrolling)
			}			
		});
		
	    TableColumn pluginTableColumnImage = new TableColumn(pluginTable, SWT.CENTER);
	    pluginTableColumnImage.setWidth(30);

	    TableColumn pluginTableColumnText = new TableColumn(pluginTable,  SWT.LEFT);
	    pluginTableColumnText.setWidth(320); // Only relevant on Windows
	    
	    for (TransferPlugin plugin : plugins) {	   	    	
	    	String pluginImageResource = String.format(PLUGIN_ICON_RESOURCE_FORMAT, plugin.getId());
	    	Image image = SWTResourceManager.getImage(pluginImageResource);

	    	TableItem tableItem = new TableItem(pluginTable, SWT.NONE);		    
	    	tableItem.setImage(0, image);
	    	tableItem.setText(1, plugin.getName());		    
	    	tableItem.setData(plugin);
	    }	    	   
	}

	public TransferPlugin getSelectedPlugin() {
		return selectedPlugin;
	}

	public void clearSelection() {
		pluginTable.deselectAll();
		selectedPlugin = null;
	}		
}
