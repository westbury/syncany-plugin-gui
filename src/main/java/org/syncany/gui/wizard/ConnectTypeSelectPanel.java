/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.gui.wizard;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.gui.Panel;
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.WidgetDecorator;
import org.syncany.operations.init.ApplicationLink;
import org.syncany.api.transfer.StorageException;
import org.syncany.api.transfer.TransferPlugin;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ConnectTypeSelectPanel extends Panel {
	private static final Logger logger = Logger.getLogger(ConnectTypeSelectPanel.class.getSimpleName());	

	public enum ConnectPanelSelection {
		LINK, MANUAL
	}

	private Button connectLinkRadio;
	private StyledText connectLinkText;
	private Button connectManuallyRadio;
	private PluginSelectComposite pluginSelectComposite;
	
	private boolean firstValidationDone;	
	private ApplicationLink applicationLink;

	public ConnectTypeSelectPanel(WizardDialog parentDialog, Composite composite, int style) {
		super(parentDialog, composite, style);

		this.createControls();
		this.firstValidationDone = false;
	}
	
	private void createControls() {
		// Main composite
		GridLayout mainCompositeGridLayout = new GridLayout(1, false);
		mainCompositeGridLayout.marginTop = 15;
		mainCompositeGridLayout.marginLeft = 10;
		mainCompositeGridLayout.marginRight = 20;
		mainCompositeGridLayout.marginBottom = 10;

		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		setLayout(mainCompositeGridLayout);

		// Title and welcome text
		Label titleLabel = new Label(this, SWT.WRAP);
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		titleLabel.setText(I18n.getText("org.syncany.gui.wizard.ConnectTypeSelectPanel.title"));

		WidgetDecorator.title(titleLabel);

		Label descriptionLabel = new Label(this, SWT.WRAP);
		descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
		descriptionLabel.setText(I18n.getText("org.syncany.gui.wizard.ConnectTypeSelectPanel.description"));
		
		WidgetDecorator.normal(descriptionLabel);

		// Radio button "Create new repo"
		GridData connectLinkRadioGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		connectLinkRadioGridData.verticalIndent = 0;
		connectLinkRadioGridData.horizontalIndent = 0;

		connectLinkRadio = new Button(this, SWT.RADIO);
		connectLinkRadio.setLayoutData(connectLinkRadioGridData);
		connectLinkRadio.setBounds(0, 0, 90, 16);
		connectLinkRadio.setText(I18n.getText("org.syncany.gui.wizard.ConnectTypeSelectPanel.link.title"));
		connectLinkRadio.setSelection(true);
		
		connectLinkRadio.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				connectLinkText.setForeground(WidgetDecorator.BLACK);
				pluginSelectComposite.clearSelection();

				validatePanelIfFirstValidationDone();				
			}			
		});

		WidgetDecorator.bigger(connectLinkRadio);

		GridData connectLinkTextGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		connectLinkTextGridData.horizontalIndent = 25;
		connectLinkTextGridData.minimumHeight = 80;

		final String connectLinkTextDefaultValue = I18n.getText("org.syncany.gui.wizard.ConnectTypeSelectPanel.link.pasteLinkHere");
		
		connectLinkText = new StyledText(this, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		connectLinkText.setLayoutData(connectLinkTextGridData);
		connectLinkText.setText(connectLinkTextDefaultValue);
		connectLinkText.setForeground(WidgetDecorator.GRAY);		
		connectLinkText.setBackground(WidgetDecorator.WHITE);
		
		// No live validation, because short-links are resolved against syncany.org!
		
		connectLinkText.addFocusListener(new FocusListener() {			
			@Override
			public void focusGained(FocusEvent e) {
				if (connectLinkText.getText().equals(connectLinkTextDefaultValue)) {
					connectLinkText.setText("");
				}
				
				connectLinkRadio.setSelection(true);
				connectLinkText.setForeground(WidgetDecorator.BLACK);	

				connectManuallyRadio.setSelection(false);								

				validatePanelIfFirstValidationDone();
			}
			
			@Override
			public void focusLost(FocusEvent e) {
				if (connectLinkText.getText().equals("")) {
					connectLinkText.setText(connectLinkTextDefaultValue);
				}
			}			
		});

		// Radio button "Connect to existing repo"
		GridData connectManuallyRadioGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		connectManuallyRadioGridData.verticalIndent = 23;
		connectManuallyRadioGridData.horizontalIndent = 0;
		connectManuallyRadioGridData.heightHint = 20;

		connectManuallyRadio = new Button(this, SWT.RADIO);
		connectManuallyRadio.setLayoutData(connectManuallyRadioGridData);
		connectManuallyRadio.setBounds(0, 0, 90, 16);
		connectManuallyRadio.setText(I18n.getText("org.syncany.gui.wizard.ConnectTypeSelectPanel.manually.title"));
		connectManuallyRadio.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				connectLinkText.setForeground(WidgetDecorator.GRAY);	
				validatePanelIfFirstValidationDone();
			}			
		});
		
		WidgetDecorator.bigger(connectManuallyRadio);
		
		GridData pluginSelectCompositeGridData = new GridData(GridData.FILL_BOTH);
		pluginSelectCompositeGridData.horizontalIndent = 25;
		pluginSelectCompositeGridData.minimumHeight = 40;
		
		pluginSelectComposite = new PluginSelectComposite(this, SWT.NONE);
		pluginSelectComposite.setLayoutData(pluginSelectCompositeGridData);		
		pluginSelectComposite.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				connectLinkRadio.setSelection(false);
				connectLinkText.setForeground(WidgetDecorator.GRAY);	

				connectManuallyRadio.setSelection(true);
				
				validatePanelIfFirstValidationDone();
			}
		});
	}

	@Override
	public boolean validatePanel() {
		firstValidationDone = true;
		
		if (connectLinkRadio.getSelection()) {
			try {
				applicationLink = new ApplicationLink(connectLinkText.getText());
				
				WidgetDecorator.markAsValid(connectLinkText);
				return true;
			}
			catch (StorageException | IllegalArgumentException e) {
				logger.log(Level.WARNING, "Validation of link failed.", e);
				
				WidgetDecorator.markAsInvalid(connectLinkText);
				return false;
			}			
		}
		else {
			WidgetDecorator.markAsValid(connectLinkText);
			return pluginSelectComposite.getSelectedPlugin() != null;
		}
	}
	
	private void validatePanelIfFirstValidationDone() {
		if (firstValidationDone) {
			validatePanel();
		}		
	}

	public ConnectPanelSelection getSelection() {
		if (connectLinkRadio.getSelection()) {
			return ConnectPanelSelection.LINK;
		}
		else {
			return ConnectPanelSelection.MANUAL;
		}
	}
	
	public TransferPlugin getSelectedPlugin() {
		return pluginSelectComposite.getSelectedPlugin();
	}
	
	public String getApplicationLinkText() {
		return connectLinkText.getText();
	}

	public ApplicationLink getApplicationLink() {
		return applicationLink;
	}
}
