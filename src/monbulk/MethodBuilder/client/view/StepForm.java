package monbulk.MethodBuilder.client.view;

import java.util.ArrayList;




import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;



import monbulk.shared.Architecture.IView.IFormView;
import monbulk.shared.Architecture.IView.IDraggable;
import monbulk.shared.Form.FormBuilder;
import monbulk.shared.Form.FormWidget;
import monbulk.shared.Model.pojo.pojoStepDetails;
import monbulk.shared.Model.pojo.pojoStudy;


public class StepForm extends SubjectPropertiesForm implements IFormView,IDraggable {

	private int AddedRowIndex; 
	
	
	
	public StepForm() {
		
		super();
				
		//Modality.
	}

	@Override
	public void LoadForm(FormBuilder someForm) {
		super.LoadForm(someForm);
		FormWidget tmpWidg = this.getFormWidgetForName(pojoStepDetails.HasStudyField);
		
		if(!tmpWidg.equals(null))
		{
			tmpWidg.getFormWidget().addHandler(new ValueChangeHandler<Boolean>()
			{
	
				@Override
				public void onValueChange(ValueChangeEvent<Boolean> event) {
					// TODO Auto-generated method stub
					if(event.getValue())
					{
						showStudyDetails();
					}
					else
					{
						hideStudyDetails();
					}
					//event.getValue();
				}
				
			},null);
		}

	}
	@Override
	public void setData(ArrayList<String> someList) {
		// TODO Auto-generated method stub
		
	}

	private final void hideStudyDetails()
	{
		this.getFormWidgetForName(pojoStudy.DicomModalityField).disable();
		this.getFormWidgetForName(pojoStudy.StudyTypeField).disable();
		
	}
	private final void showStudyDetails()
	{
		this.getFormWidgetForName(pojoStudy.DicomModalityField).enable();
		this.getFormWidgetForName(pojoStudy.StudyTypeField).enable();
		
	}

	


	
}
