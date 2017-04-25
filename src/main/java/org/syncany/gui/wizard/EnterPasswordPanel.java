package org.syncany.gui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.gui.Panel;
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.util.WidgetDecorator;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class EnterPasswordPanel extends Panel {
	private Text passwordText;
	
	private Label warningImageLabel;
	private Label warningMessageLabel;
	
	private boolean firstValidationDone;

	public EnterPasswordPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);		
		
		this.createControls();
		this.firstValidationDone = false;
	}
				
	private void createControls() {
		// Main composite
		GridLayout mainCompositeGridLayout = new GridLayout(3, false);
		mainCompositeGridLayout.marginTop = 15;
		mainCompositeGridLayout.marginLeft = 10;
		mainCompositeGridLayout.marginRight = 20;

		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));		
		setLayout(mainCompositeGridLayout);		
	
		// Title and description
		Label titleLabel = new Label(this, SWT.WRAP);
		titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		titleLabel.setText(I18n.getText("org.syncany.gui.wizard.EnterPasswordPanel.title"));
		
		WidgetDecorator.title(titleLabel);

		Label descriptionLabel = new Label(this, SWT.WRAP);
		descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
		descriptionLabel.setText(I18n.getText("org.syncany.gui.wizard.EnterPasswordPanel.description"));

		WidgetDecorator.normal(descriptionLabel);

		// Label "Password:"
		GridData passwordLabelGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		passwordLabelGridData.verticalIndent = WidgetDecorator.VERTICAL_INDENT;
		passwordLabelGridData.horizontalSpan = 3;

		Label passwordLabel = new Label(this, SWT.WRAP);
		passwordLabel.setLayoutData(passwordLabelGridData);
		passwordLabel.setText(I18n.getText("org.syncany.gui.wizard.EnterPasswordPanel.passwordLabel"));
		
		// Textfield "Password"
		GridData passwordTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		passwordTextGridData.verticalIndent = 0;
		passwordTextGridData.horizontalSpan = 3;
		passwordTextGridData.minimumWidth = 200;

		passwordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(passwordTextGridData);
		passwordText.setBackground(WidgetDecorator.WHITE);
		passwordText.addModifyListener(new ModifyListener() {			
			@Override
			public void modifyText(ModifyEvent event) {
				if (firstValidationDone) {
					validatePanel();
				}
			}
		});
		
		WidgetDecorator.normal(passwordText);		
		
		// Warning message and label
		String warningImageResource = "/" + WizardDialog.class.getPackage().getName().replace(".", "/") + "/warning-icon.png";
		Image warningImage = SWTResourceManager.getImage(warningImageResource);

		warningImageLabel = new Label(this, SWT.NONE);
		warningImageLabel.setImage(warningImage);
		warningImageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		warningImageLabel.setVisible(false);

		warningMessageLabel = new Label(this, SWT.WRAP);
		warningMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		warningMessageLabel.setVisible(false);
		
		WidgetDecorator.bold(warningMessageLabel);
	}

	@Override
	public boolean validatePanel() {
		firstValidationDone = true;
		
		boolean validPassword = passwordText.getText().length() >= 10;
		
		if (!validPassword) {
			WidgetDecorator.markAsInvalid(passwordText);
			showWarning(I18n.getText("org.syncany.gui.wizard.EnterPasswordPanel.errorTooShort"));
			
			return false;			
		}
		else {
			WidgetDecorator.markAsValid(passwordText);			
			hideWarning();
			
			return true;
		}
	}
	
	private void showWarning(String warningStr) {
		warningImageLabel.setVisible(true);
		warningMessageLabel.setVisible(true);			
		warningMessageLabel.setText(warningStr);
	}
	
	private void hideWarning() {
		warningImageLabel.setVisible(false);
		warningMessageLabel.setVisible(false);
	}

	public String getPassword() {
		return passwordText.getText();
	}
}
