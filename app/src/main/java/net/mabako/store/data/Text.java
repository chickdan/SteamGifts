package net.mabako.store.data;

import net.mabako.steamgifts.R;
import net.mabako.steamgifts.adapters.IEndlessAdaptable;

/**
 * Created by mabako on 10.01.2016.
 */
public class Text implements IEndlessAdaptable {
    public static final int VIEW_LAYOUT = R.layout.text_item;

    private final String text;
    private final boolean html;

    public Text(String text, boolean html) {
        this.text = text;
        this.html = html;
    }

    public String getText() {
        return text;
    }

    public boolean isHtml() {
        return html;
    }

    @Override
    public int getLayout() {
        return VIEW_LAYOUT;
    }
}
