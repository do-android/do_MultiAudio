package dotest.module.do_MultiAudio.debug;

import android.content.Context;

import dotest.module.do_MultiAudio.ui.DoTestSampleModel;
import dotest.module.do_MultiAudio.ui.DoTestSampleView;

import core.interfaces.DoIPage;
import core.interfaces.DoIUIModuleFactory;
import core.interfaces.DoIUIModuleGroup;
import core.interfaces.DoIUIModuleView;
import core.object.DoUIModule;

public class DoUIModuleFactory implements DoIUIModuleFactory {

	private Context mContext;
	
	public DoUIModuleFactory(Context context){
		this.mContext = context;
	}
	
	@Override
	public DoUIModule createUIModule(String _typeID) throws Exception {
		return null;
	}

	@Override
	public void bindUIModuleView(DoUIModule _uiModule) throws Exception {
		
	}

	@Override
	public void registGroup(DoIUIModuleGroup _uiModuleGroup) {
		
	}

	@Override
	public DoUIModule createUIModuleBySourceFile(String path,
			DoIPage _page, boolean isLoadScript) throws Exception {
		DoTestSampleModel sampleModel = new DoTestSampleModel();
		DoIUIModuleView sampleView = new DoTestSampleView(mContext);
		sampleModel.setCurrentUIModuleView(sampleView);
		return sampleModel;
	}

}
