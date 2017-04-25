package org.syncany.gui.wizard;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.syncany.api.FileType;
import org.syncany.api.transfer.PropertyVisitor;
import org.syncany.api.transfer.Setter;
import org.syncany.api.transfer.TransferPlugin;
import org.syncany.api.transfer.TransferSettings;
import org.syncany.api.transfer.TransferSettingsSetter;
import org.syncany.gui.Panel;
import org.syncany.gui.util.I18n;
import org.syncany.gui.util.SWTResourceManager;
import org.syncany.gui.util.WidgetDecorator;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class PluginSettingsPanel extends Panel {
	private static final Logger logger = Logger.getLogger(PluginSettingsPanel.class.getSimpleName());

	private Label warningImageLabel;
	private Label warningMessageLabel;

	private TransferPlugin plugin;
	private TransferSettings pluginSettings;
	private static PluginSettingsPanelOAuthHelper pluginSettingsPanelOAuthHelper;

	private Set<Control> invalidPluginOptions = new HashSet<>();
	
	public PluginSettingsPanel(WizardDialog wizardParentDialog, Composite parent, int style) {
		super(wizardParentDialog, parent, style);
	}

	@Override
	public void dispose() {
		logger.log(Level.INFO, "PluginSettingsPanel is about to get disposed, resetting OAuthhelper");
		resetOAuthHelper();
	}

	public void init(TransferPlugin plugin) {
		setPlugin(plugin);

		resetOAuthHelper();
		clearControls();

		createControls();
	}

	private void setPlugin(TransferPlugin plugin) {
		try {
			this.plugin = plugin;
			this.pluginSettings = plugin.createEmptySettings();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void clearControls() {
		for (Control childComponent : getChildren()) {
			childComponent.dispose();
		}
	}

	private void resetOAuthHelper() {
		if (pluginSettingsPanelOAuthHelper != null) {
			pluginSettingsPanelOAuthHelper.reset(false);
			pluginSettingsPanelOAuthHelper = null;
		}
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
		titleLabel.setText(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.title", plugin.getName()));

		WidgetDecorator.title(titleLabel);

		// Create OAuth controls (if any)
		createOAuthControls();

		// Create fields
		pluginSettings.visitProperties(new PropertyVisitor() {

			@Override
			public void stringProperty(String id, String displayName, boolean isRequired, boolean storeEncrypted, boolean sensitive, boolean singular,
					boolean visible, Supplier<String> value, Consumer<String> setter) {
				if (visible) {
					createPluginOptionControl(displayName, sensitive, isRequired);
					createPluginOptionTextControl(displayName, isRequired, sensitive, value.get(), setter);					
				}
			}

			@Override
			public void integerProperty(String id, String displayName, boolean isRequired, boolean storeEncrypted, boolean sensitive,
					boolean singular, boolean visible, Supplier<Integer> value, Consumer<Integer> setter) {
				if (visible) {
					createPluginOptionControl(displayName, sensitive, isRequired);
					createPluginOptionIntegerControl(sensitive, value.get(), setter);					
				}
			}

			@Override
			public void booleanProperty(String id, String displayName, boolean isRequired, boolean singular, boolean visible, boolean value,
					Setter<Boolean> setter) {
				if (visible) {
					createPluginOptionControl(displayName, false, isRequired);
					createPluginOptionBooleanControl(value, setter);					
				}
			}

			@Override
			public void fileProperty(String id, String displayName, boolean isRequired, boolean singular, boolean visible,
					FileType fileType, File value, Setter<File> setter) {
				if (visible) {
					createPluginOptionControl(displayName, false, isRequired);
					createPluginOptionFileControl(displayName, isRequired, fileType, value, setter);						
				}
			}

			@Override
			public <T extends Enum<T>> void enumProperty(String id, String displayName, boolean isRequired, T[] options, T value, Setter<T> setter) {
				createPluginOptionControl(displayName, false, isRequired);
				createPluginOptionEnumControl(options, value, setter);			
			}

			@Override
			public void nestedSettingsProperty(String id, String displayName, boolean isRequired, TransferSettingsSetter<?> setter) {
				// TODO
			}
		});
		
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

		pack();
	}

	private void createOAuthControls() {
		resetOAuthHelper();

		PluginSettingsPanelOAuthHelper.Builder builder;

		try {
			builder = PluginSettingsPanelOAuthHelper.forSettings(pluginSettings);
		}
		catch (UnsupportedOperationException e) {
			// ok plugin does not support oauth
			return;
		}

		// OAuth help text
		Label descriptionLabel = new Label(this, SWT.WRAP);
		descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 3, 1));
		descriptionLabel.setText(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.oauth.description"));

		WidgetDecorator.normal(descriptionLabel);

		// Label "Token:"
		GridData oAuthTokenLabelGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		oAuthTokenLabelGridData.verticalIndent = 2;
		oAuthTokenLabelGridData.horizontalSpan = 3;

		Label oAuthTokenLabel = new Label(this, SWT.WRAP);
		oAuthTokenLabel.setLayoutData(oAuthTokenLabelGridData);
		oAuthTokenLabel.setText(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.oauth.token"));

		// Textfield "Token"
		GridData oAuthTokenTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		oAuthTokenTextGridData.verticalIndent = 0;
		oAuthTokenTextGridData.horizontalSpan = 2;
		oAuthTokenTextGridData.minimumWidth = 200;
		oAuthTokenTextGridData.grabExcessHorizontalSpace = true;

		// Do not manage contents of the following GUI items, done by OAuthInformationManager
		Text oAuthTokenText = new Text(this, SWT.BORDER);
		oAuthTokenText.setLayoutData(oAuthTokenTextGridData);
		oAuthTokenText.setBackground(WidgetDecorator.WHITE);

		// Add 'Authorize ..' button for 'File' fields
		Button oAuthAuthorizeButton = new Button(this, SWT.NONE);
		oAuthAuthorizeButton.setText(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.oauth.button.connecting")); // needs text for size

		try {
			pluginSettingsPanelOAuthHelper = builder
							.withWarningHandler(new WarningHandler())
							.withButton(oAuthAuthorizeButton)
							.withText(oAuthTokenText)
							.build();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		pluginSettingsPanelOAuthHelper.start();
	}

	private void createPluginOptionControl(String displayName, boolean sensitive, boolean isRequired) {
		// Label "Option X:"
		GridData pluginOptionLabelGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		pluginOptionLabelGridData.verticalIndent = 2;
		pluginOptionLabelGridData.horizontalSpan = 3;

		String pluginOptionLabelText = displayName;

		if (sensitive) {
			pluginOptionLabelText += " " + (isRequired
							? I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.pluginOptionLabelExt.notDisplayed")
							: I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.pluginOptionLabelExt.notDisplayedOptional"));
		}
		else {
			pluginOptionLabelText += isRequired ? "" : " " + I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.pluginOptionLabelExt.optional");
		}

		Label pluginOptionLabel = new Label(this, SWT.WRAP);
		pluginOptionLabel.setLayoutData(pluginOptionLabelGridData);
		pluginOptionLabel.setText(pluginOptionLabelText);
	}

	private void createPluginOptionFileControl(String displayName, boolean isRequired, FileType fileType, File value, Setter<File> setter) {
		// Create controls
		
		Consumer<String> stringSetter = new Consumer<String>() {
			@Override
			public void accept(String value) {
				setter.setValue(new File(value));
			}
		};
		
		Text pluginOptionValueText = createPluginOptionTextField(displayName, isRequired, false, value.getAbsolutePath(), stringSetter, 2);
		createPluginOptionFileSelectButton(fileType, setter, pluginOptionValueText);
	}
	
	private <T extends Enum<T>> void createPluginOptionEnumControl(T[] options, T value, Setter<T> setter) {
		Combo combo = new Combo(this, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		for (T enumValue : options) {
			combo.add(enumValue.name());				
		}					
		
		combo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				T newValue = options[combo.getSelectionIndex()];
				setter.setValue(newValue);
			}
		});

		int comboIndex = Arrays.asList(options).indexOf(value);
		combo.select(comboIndex);			
	}

	private void createPluginOptionTextControl(String displayName, boolean isRequired, boolean sensitive, String value, Consumer<String> setter) {
		createPluginOptionTextField(displayName, isRequired, sensitive, value, setter, 3);
	}

	private Text createPluginOptionTextField(String displayName, boolean isRequired, boolean sensitive, String initialValue, Consumer<String> setter, int horizontalSpan) {
		// Textfield "Option X"
		GridData optionValueTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		optionValueTextGridData.verticalIndent = 0;
		optionValueTextGridData.horizontalSpan = horizontalSpan;
		optionValueTextGridData.minimumWidth = 200;
		optionValueTextGridData.grabExcessHorizontalSpace = true;

		int optionValueTextStyle = sensitive ? SWT.BORDER | SWT.PASSWORD : SWT.BORDER;

		Text pluginOptionValueText = new Text(this, optionValueTextStyle);
		pluginOptionValueText.setLayoutData(optionValueTextGridData);
		pluginOptionValueText.setBackground(WidgetDecorator.WHITE);

		if (initialValue != null) {
			pluginOptionValueText.setText(initialValue);
		}
		pluginOptionValueText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (isRequired && pluginOptionValueText.getText().isEmpty()) {
					invalidPluginOptions.add(pluginOptionValueText);
					WidgetDecorator.markAsInvalid(pluginOptionValueText);
				}
				else {
					invalidPluginOptions.remove(pluginOptionValueText);
					WidgetDecorator.markAsValid(pluginOptionValueText);
				}

					if (pluginOptionValueText.getText().isEmpty()) {
						setter.accept(null);
					} else {
						setter.accept(pluginOptionValueText.getText());
					}
//				} catch (StorageException e2) {
//					if (!sensitive) {
//						logger.log(Level.WARNING, "Cannot set field '" + displayName + "' with value '" + pluginOptionValueText.getText() + "'", e);
//					}
//					else {
//						logger.log(Level.WARNING, "Cannot set field '" + displayName + "' with sensitive value.");
//					}
//					invalidPluginOptions.add(pluginOptionValueText);
//					WidgetDecorator.markAsInvalid(pluginOptionValueText);
//				}
			}
		});

		WidgetDecorator.normal(pluginOptionValueText);
		
		return pluginOptionValueText;
	}

	private void createPluginOptionIntegerControl(boolean sensitive, int initialValue, Consumer<Integer> setter) {
		// Textfield "Option X"
		GridData optionValueTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		optionValueTextGridData.verticalIndent = 0;
		optionValueTextGridData.horizontalSpan = 3;
		optionValueTextGridData.minimumWidth = 200;
		optionValueTextGridData.grabExcessHorizontalSpace = true;

		int optionValueTextStyle = sensitive ? SWT.BORDER | SWT.PASSWORD : SWT.BORDER;

		Text pluginOptionValueText = new Text(this, optionValueTextStyle);
		pluginOptionValueText.setLayoutData(optionValueTextGridData);
		pluginOptionValueText.setBackground(WidgetDecorator.WHITE);

		pluginOptionValueText.setText(Integer.toString(initialValue));

		pluginOptionValueText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				String newValue = pluginOptionValueText.getText();
				if (newValue.isEmpty()) {
					setter.accept(0);
				} else {
					try {
						setter.accept(Integer.valueOf(newValue));

						invalidPluginOptions.remove(pluginOptionValueText);
						WidgetDecorator.markAsValid(pluginOptionValueText);
					} catch (NumberFormatException e) {
						invalidPluginOptions.add(pluginOptionValueText);
						WidgetDecorator.markAsInvalid(pluginOptionValueText);
					}
				}
			}
		});

		WidgetDecorator.normal(pluginOptionValueText);
	}

	private void createPluginOptionBooleanControl(boolean initialValue, Setter<Boolean> setter) {
		// Textfield "Option X"
		GridData optionValueTextGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		optionValueTextGridData.verticalIndent = 0;
		optionValueTextGridData.horizontalSpan = 3;
		optionValueTextGridData.minimumWidth = 200;
		optionValueTextGridData.grabExcessHorizontalSpace = true;

		Button pluginOptionValueText = new Button(this, SWT.CHECK);
		pluginOptionValueText.setLayoutData(optionValueTextGridData);
		pluginOptionValueText.setBackground(WidgetDecorator.WHITE);

		pluginOptionValueText.setSelection(initialValue);

		pluginOptionValueText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setter.setValue(pluginOptionValueText.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				setter.setValue(pluginOptionValueText.getSelection());
			}
		});

		WidgetDecorator.normal(pluginOptionValueText);
	}

	private void createPluginOptionFileSelectButton(FileType fileType, Setter<File> setter, Text pluginOptionValueText) {
		Button pluginOptionFileSelectButton = new Button(this, SWT.NONE);
		pluginOptionFileSelectButton.setText(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.selectFile"));

		pluginOptionFileSelectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				onSelectFileClick(fileType, setter, pluginOptionValueText);
			}
		});
	}

	private void onSelectFileClick(FileType fileType, Setter<File> setter, Text pluginOptionValueText) {
		if (fileType == FileType.FILE) {
			String filterPath = new File(pluginOptionValueText.getText()).getParent();

			FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
			fileDialog.setFilterExtensions(new String[]{"*.*"});
			fileDialog.setFilterPath(filterPath);

			String selectedFile = fileDialog.open();

			if (selectedFile != null && selectedFile.length() > 0) {
				pluginOptionValueText.setText(selectedFile);
			}
		}
		else {
			DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
			directoryDialog.setFilterPath(pluginOptionValueText.getText());

			String selectedFolder = directoryDialog.open();

			if (selectedFolder != null && selectedFolder.length() > 0) {
				pluginOptionValueText.setText(selectedFolder);
			}
		}
	}

	@Override
	public boolean validatePanel() {
		hideWarning();

		logger.log(Level.INFO, "Validating settings panel ...");

		// Validation order is important, because the validate*() methods
		// also mark fields 'red'. Also: OAuth needs to be before
		// cross-field dependencies!

		boolean individualFieldsValid = validateIndividualFields();
		boolean oAuthFieldsValid = validateOAuthToken();

		return individualFieldsValid && oAuthFieldsValid && validateFieldDependencies();
	}

	private boolean validateIndividualFields() {
		logger.log(Level.INFO, " - Validating individual fields ...");

		boolean validFields = invalidPluginOptions.size() == 0;

		if (validFields) {
			return true;
		}
		else {
			showWarning(I18n.getText("org.syncany.gui.wizard.PluginSettingsPanel.errorFieldValidation"));
			return false;
		}
	}

	private boolean validateFieldDependencies() {
		logger.log(Level.INFO, " - Validating field dependencies ...");

		boolean isValid = pluginSettings.isValid();
		if (isValid) {
			logger.log(Level.INFO, "Validation succeeded on panel.");
			return true;
		} else {
			showWarning(pluginSettings.getReasonForLastValidationFail());

			logger.log(Level.WARNING, "Validate error on panel.");
			return false;
		}
	}

	private boolean validateOAuthToken() {
		return pluginSettingsPanelOAuthHelper == null || pluginSettingsPanelOAuthHelper.isSuccess();
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

	public TransferSettings getPluginSettings() {
		return pluginSettings;
	}

	private class WarningHandler implements Consumer<String> {
		@Override
		public void accept(final String warning) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					PluginSettingsPanel.this.showWarning(warning);
				}
			});
		}
	}
}
