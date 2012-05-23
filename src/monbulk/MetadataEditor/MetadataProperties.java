package monbulk.MetadataEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

import monbulk.client.Monbulk;
import monbulk.client.Settings;
import monbulk.client.desktop.Desktop;
import monbulk.shared.Services.Metadata;
import monbulk.shared.Services.MetadataService;
import monbulk.shared.Services.MetadataService.GetMetadataHandler;
import monbulk.shared.widgets.Window.OkCancelWindow.*;
import monbulk.shared.widgets.Window.WindowSettings;
import monbulk.shared.widgets.TextBoxEx;

public class MetadataProperties extends Composite implements SelectionHandler<TreeItem>, CommonElementPanel.ChangeTypeHandler, OkCancelHandler, ValidateHandler
{
	private static MetadataPropertiesUiBinder uiBinder = GWT.create(MetadataPropertiesUiBinder.class);
	interface MetadataPropertiesUiBinder extends UiBinder<Widget, MetadataProperties> { }

	@UiField Label m_namespace;
	@UiField TextBoxEx m_name;
	@UiField TextBox m_label;
	@UiField TextBox m_description;
	@UiField Tree m_elementsTree;
	@UiField Button m_addElement;
	@UiField Button m_removeElement;
	@UiField Button m_editElement;
	@UiField HTMLPanel m_addRemovePanel;
	@UiField CaptionPanel m_elements;
	@UiField Label m_elementType;
	@UiField Label m_elementDescription;
	@UiField Button m_save;
	@UiField Button m_saveAsTemplate;

	private TreeItem m_selectedElement = null;
	private Metadata m_metadata = null;
	private ElementEditor m_elementEditor;
	private boolean m_addNewElement = false;
	
	public MetadataProperties() throws Exception
	{
		initWidget(uiBinder.createAndBindUi(this));
		m_elementsTree.addSelectionHandler(this);

		// Register our own element editor window.
		m_elementEditor = new ElementEditor(true);
		m_elementEditor.setChangeTypeHandler(this);
		m_elementEditor.setValidateHandler(this);
		WindowSettings w = m_elementEditor.getWindowSettings();
		w.windowId = "ElementEditor-Main";
		w.windowTitle = "Element";
		Desktop.get().registerWindow(m_elementEditor);

		// Only allow upper and lower letters, full stop, and hyphen.
		m_name.setValidCharRegex("[a-zA-Z.-]");
		
		Settings settings = Monbulk.getSettings();
		String namespace = settings.getDefaultNamespace();
		m_namespace.setText(namespace);
	}

	/**
	 * Sets this properties widget to read-only.  All ui fields will be
	 * set to read-only.
	 * 
	 * Currently this only works if you set the MetadataProperties to
	 * read-only.  If you try to set it back to not read-only the add/remove
	 * panel won't be re-added.  We don't need this functionality at the moment
	 * so I haven't implemented it.
	 * @param readOnly
	 */
	public void setReadOnly(boolean readOnly)
	{
		// FIXME: Currently this only works if you set MetadataProperties to
		// read-only.  If you try to set it back to not read-only the add/remove
		// panel won't be re-added.  We don't need this functionality at the moment
		// so I haven't implemented it.
		if (readOnly)
		{
			m_addRemovePanel.removeFromParent();
			LayoutPanel p = (LayoutPanel)getWidget();
			p.setWidgetTopBottom(m_elements, 126, Unit.PX, 0, Unit.PX);
		}
		
		m_name.setEnabled(!readOnly);
		m_label.setEnabled(!readOnly);
		m_description.setEnabled(!readOnly);
		m_elementEditor.setReadOnly(readOnly);
	}

	/**
	 * Adds a handler to the save button.
	 * @param event
	 */
	public void addSaveHandler(ClickHandler handler)
	{
		m_save.addClickHandler(handler);
	}

	/**
	 * Returns the current metadata object.
	 * @return
	 */
	public Metadata getMetadata()
	{
		return m_metadata;
	}

	/**
	 * Sets the metadata object to be edited from a metadata name.
	 * This is a convenience method to look up the metadata object.
	 * @param name
	 */
	public void setMetadata(String name)
	{
		MetadataService service = MetadataService.get();
		if (service != null)
		{
			service.getMetadata(name, new GetMetadataHandler()
			{
				// Callback for reading a specific metadata object.
				public void onGetMetadata(Metadata metadata)
				{
					setMetadata(metadata);
				}
			});
		}
	}

	/**
	 * Sets the metadata object to be edited.  Sets up all ui fields
	 * and populates the elements tree.
	 * @param metadata
	 */
	public void setMetadata(Metadata metadata)
	{
		clear();
		m_metadata = metadata;
		m_addElement.setEnabled(true);

		String name = metadata.getName();
		
		// If there is a namespace at the start, strip it off.
		Settings settings = Monbulk.getSettings();
		String namespace = settings.getDefaultNamespace() + ".";
		if (name.startsWith(namespace))
		{
			name = name.substring(namespace.length());
		}

		m_name.setText(name);
		m_label.setText(metadata.getLabel());
		m_description.setText(metadata.getDescription());
		populateElementTree(null, metadata.getRootElement());
		if (m_elementsTree.getItemCount() > 0)
		{
			// Select the first item in the tree automatically.
			TreeItem treeItem = m_elementsTree.getItem(0);
			m_elementsTree.setSelectedItem(treeItem, true);
		}
	}

	/**
	 * Resets all ui properties and empties the elements tree.
	 */
	public void clear()
	{
		m_metadata = null;
		m_addElement.setEnabled(false);
		m_name.setText("");
		m_label.setText("");
		m_description.setText("");
		m_elementsTree.clear();
		clearElements();
	}
	
	private TreeItem createTreeItem(String name, Metadata.Element element, TreeItem root)
	{
		TreeItem treeItem = new TreeItem(name);
		treeItem.addStyleName("itemHighlight");
		if (root != null)
		{
			treeItem.addStyleName("noItemHighlight");
		}
		treeItem.setUserObject(element);

		// Add new item to tree.
		if (root != null)
		{
			root.addItem(treeItem);
		}
		else
		{
			m_elementsTree.addItem(treeItem);
		}

		return treeItem;
	}
	
	private void populateElementTree(TreeItem root, Metadata.DocumentElement rootElement)
	{
		int numChildren = rootElement.getNumChildren();
		for (int i = 0; i < numChildren; i++)
		{
			Metadata.Element e = rootElement.getChild(i);
			if (e.getType().isVisible())
			{
				TreeItem newItem = createTreeItem(e.getName(), e, root);
			
				if (e instanceof Metadata.DocumentElement)
				{
					// Recurse into DocumentElements.
					Metadata.DocumentElement doc = (Metadata.DocumentElement)e;
					populateElementTree(newItem, doc);
				}
			}
		}
	}

	// -------------------------------------------
	// Button handlers
	// -------------------------------------------

	@UiHandler("m_addElement")
	public void onAddElementClicked(ClickEvent event)
	{
		try
		{
			Metadata.Element newElement = Metadata.createElement("string", "New element", "New element description", false);
			showEditor(newElement, true);
		}
		catch (Exception e)
		{
			GWT.log(e.toString());
		}
	}
	
	@UiHandler("m_removeElement")
	public void onRemoveElementClicked(ClickEvent event)
	{
		// Remove the element.
		Metadata.Element element = (Metadata.Element)m_selectedElement.getUserObject();
		Metadata.DocumentElement parent = element.getParent();
		parent.removeChild(element);

		// Find the index of the item we are removing.
		TreeItem parentItem = m_selectedElement.getParentItem();
		int index = parentItem != null ? parentItem.getChildIndex(m_selectedElement) : getTreeItemIndex(m_selectedElement);

		if (parentItem == null)
		{
			// Remove from the tree itself.
			m_elementsTree.removeItem(m_selectedElement);
		}
		else
		{
			// Remove from its parent.
			parentItem.removeItem(m_selectedElement);
		}

		clearElements();

		// Select the next sibling or parent item.
		if (index >= 0)
		{
			if (parentItem == null)
			{
				// No parent, so select the next item in the root of the tree.
				int count = m_elementsTree.getItemCount();
				if (count > 0)
				{
					// Clamp the index.
					if (index >= count) 
					{
						index = count - 1;
					}

					TreeItem item = m_elementsTree.getItem(index);
					m_elementsTree.setSelectedItem(item);
				}
			}
			else
			{
				// Parent exists, so select the next child of the parent.
				int count = parentItem.getChildCount();
				
				// If there are no more children we will select the parent.
				TreeItem item = parentItem;
				if (count > 0)
				{
					// Clamp the index.
					if (index >= count)
					{
						index = count - 1;
					}

					item = parentItem.getChild(index);
				}
				
				m_elementsTree.setSelectedItem(item);
			}
		}
	}
	
	@UiHandler("m_editElement")
	void onEditElementClicked(ClickEvent event)
	{
		Metadata.Element selectedElement = m_selectedElement != null ? (Metadata.Element)m_selectedElement.getUserObject() : null;
		if (selectedElement != null)
		{
			// We work with a clone so we can easily undo changes.
			Metadata.Element element = selectedElement.clone();
			showEditor(element, false);
		}
	}
	
	private void showEditor(Metadata.Element element, boolean addNewElement)
	{
		m_addNewElement = addNewElement;
		m_elementEditor.setMetadataElement(element);

		Desktop d = Desktop.get();
		m_elementEditor.setOkCancelHandler(this);
		d.show(m_elementEditor, true);
	}

	// -------------------------------------------
	// End of button handlers
	// -------------------------------------------

	private void clearElements()
	{
		m_selectedElement = null;
		m_elementType.setText("");
		m_elementDescription.setText("");
		setButtonState();
	}

	private int getTreeItemIndex(TreeItem item)
	{
		int count = m_elementsTree.getItemCount();
		for (int i = 0; i < count; i++)
		{
			if (m_elementsTree.getItem(i) == item)
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public void onChangeType(Metadata.Element element, String newType)
	{
		// User has changed the type of this element.

		try
		{
			// Ensure the old element is up to date.
			m_elementEditor.updateCurrentElement();

			// Create new element from old.
			Metadata.ElementTypes t = Metadata.ElementTypes.valueOf(newType);
			Metadata.Element newElement = Metadata.createElement(t.getTypeName(), element.getName(), element.getDescription(), false);

			// Pass along any settings that are common to all element types. 
			newElement.setSetting("min-occurs", element.getSetting("min-occurs", ""));
			newElement.setSetting("max-occurs", element.getSetting("max-occurs", ""));

			m_elementEditor.setMetadataElement(newElement);
		}
		catch (Exception e)
		{
			GWT.log(e.toString());
			return;
		}
	}
	
	/**
	 *  Tree view selection handler.
	 */
	public void onSelection(SelectionEvent<TreeItem> event)
	{
		// Update metadata from user settings and remove highlight from
		// previously selected item.
		if (m_selectedElement != null)
		{
			m_selectedElement.removeStyleName("itemSelected");
		}

		// Add highlight to newly selected item.
		TreeItem selectedItem = event.getSelectedItem();
		selectedItem.addStyleName("itemSelected");

		m_selectedElement = selectedItem;
		
		// Update the element summary.
		Metadata.Element element = (Metadata.Element)m_selectedElement.getUserObject();
		m_elementType.setText(element.getType().toString());
		m_elementDescription.setText(element.getSetting("description", ""));
		
		setButtonState();
	}
	
	private void setButtonState()
	{
		boolean hasSelection = m_elementsTree.getSelectedItem() != null;
		m_removeElement.setEnabled(hasSelection);
		m_editElement.setEnabled(hasSelection);
	}
	
	public void onOkCancelClicked(Event eventType)
	{
		if (eventType == Event.Ok)
		{
			Metadata.Element oldElement = m_selectedElement != null ? (Metadata.Element)m_selectedElement.getUserObject() : null;
			Metadata.Element newElement = m_elementEditor.getMetadataElement();

			if (m_addNewElement)
			{
				// User is adding a new element.  If the currently selected
				// element is not a DocumentElement fall back to the root
				// metadata element.
				Metadata.Element parent = oldElement != null && oldElement instanceof Metadata.DocumentElement ? (Metadata.DocumentElement)oldElement : m_metadata.getRootElement();
				
				// Parent is of type DocumentElement.
				assert(parent instanceof Metadata.DocumentElement);
				newElement.setParent((Metadata.DocumentElement)parent);
				
				// Adding a new element so create a tree item for it.
				// The parent of the tree item will be either the current
				// selection or null (it will be added to root of the tree).
				TreeItem newItem = createTreeItem(newElement.getName(), newElement, parent == oldElement ? m_selectedElement : null);
				m_elementsTree.setSelectedItem(newItem, true);
				m_elementsTree.ensureSelectedItemVisible();

				// HACK: We have to set the item selected again to
				// really make sure it is visible because of this bug:
				// http://code.google.com/p/google-web-toolkit/issues/detail?id=1783
				m_elementsTree.setSelectedItem(m_selectedElement, true);
			}
			else
			{
				// User is editing an existing element.  Replace old element with
				// new element in parent.
				Metadata.DocumentElement docParent = oldElement.getParent();
				docParent.replaceChild(oldElement, newElement);
				
				if (oldElement instanceof Metadata.DocumentElement && !(newElement instanceof Metadata.DocumentElement))
				{
					// Old element was a document and new one isn't.  Remove all
					// child tree items from the current item.
					m_selectedElement.removeItems();
				}
			}

			// Update the tree item and select the element.
			m_selectedElement.setUserObject(newElement);
			m_selectedElement.setText(newElement.getName());
			m_elementsTree.setSelectedItem(m_selectedElement, true);
		}
	}
	
	@UiHandler({"m_name", "m_label", "m_description"})
	void onKeyUp(KeyUpEvent event)
	{
		if (m_metadata != null)
		{
			String name = m_name.getText();

			// Prefix the name with the namespace (unless user has already done it).
			Settings settings = Monbulk.getSettings();
			String namespace = settings.getDefaultNamespace() + ".";
			if (!name.startsWith(namespace))
			{
				name = namespace + name;
			}

			m_metadata.setName(name);
			m_metadata.setLabel(m_label.getText());
			m_metadata.setDescription(m_description.getText());
		}
	}
	
	/**
	 * Gives the name field focus and selects all text in the field.
	 */
	public void setNameFocus()
	{
		m_name.setFocus(true);
		m_name.selectAll();
	}
	
	public boolean validate()
	{
		// If there is already an element with the same name
		// as the element we are editing, show the user an
		// error message and don't let them continue.

		// First update the current element so it reflects the ui.
		// Note: we can do this because we work with a clone, so
		// updating here won't cause any changes to be committed.
		m_elementEditor.updateCurrentElement();

		Metadata.Element oldElement = m_selectedElement != null ? (Metadata.Element)m_selectedElement.getUserObject() : null;
		Metadata.Element newElement = m_elementEditor.getMetadataElement();
		String newName = newElement.getName();
		Metadata.DocumentElement parent = null;
		if (m_addNewElement)
		{
			// Adding a new element so we check either the current selection
			// or the root element for duplicates.
			parent = (oldElement != null && oldElement instanceof Metadata.DocumentElement) ? (Metadata.DocumentElement)oldElement : m_metadata.getRootElement();

			// There is no old element if we're adding a new element.
			oldElement = null;
		}
		else
		{
			// Editing an existing item so we check the parent of the item.
			parent = oldElement.getParent();
		}

		// Check the parent's children for any similarly named elements.
		int numChildren = parent.getNumChildren();
		for (int i = 0; i < numChildren; i++)
		{
			Metadata.Element child = parent.getChild(i);
			if (child != oldElement && child.getName().equalsIgnoreCase(newName))
			{
				Window.alert("There is already an element with the name '" + newName + "'.  Please enter a new name.");
				m_elementEditor.setNameFocus();
				return false;
			}
		}

		return true;
	}
}
