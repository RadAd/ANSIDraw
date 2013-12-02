package au.radsoft.ansidraw;

import au.radsoft.textui.*;

class NewDlg extends Dialog
{
    private EditField width_;
    private EditField height_;
    private final Button okButton_;
    private final Button cancelButton_;
    
    NewDlg(int w, int h)
    {
        super(1, 1, 23, 7);
        setTitle("New");
        add(new StringField(1, 1, "Width:"));
        add(width_ = new EditField(12, 1, Integer.toString(w), 10));
        add(new StringField(1, 3, "Height:"));
        add(height_ = new EditField(12, 3, Integer.toString(h), 10));
        
        add(okButton_ = new Button(w_ - 22, h_ - 2, "Ok"));
        add(cancelButton_ = new Button(w_ - 11, h_ - 2, "Cancel"));
        
        setActive(null, width_);
    }
    
    int getWidthValue()
    {
        return Integer.parseInt(width_.getValue());
    }
    
    int getHeightValue()
    {
        return Integer.parseInt(height_.getValue());
    }
    
    @Override
	public void onEvent(ConsoleState state, Button.Event bev)
	{
        super.onEvent(state, bev);
        
        if (bev.button == okButton_)
            state.result(1);
        state.exit();
	}
}
