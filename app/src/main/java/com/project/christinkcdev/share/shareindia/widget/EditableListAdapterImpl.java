package com.project.christinkcdev.share.shareindia.widget;

import com.genonbeta.android.framework.widget.ListAdapterImpl;
import com.project.christinkcdev.share.shareindia.exception.NotReadyException;
import com.project.christinkcdev.share.shareindia.models.Editable;

import java.util.List;

public interface EditableListAdapterImpl<T extends Editable> extends ListAdapterImpl<T> {
    boolean filterItem(T item);

    T getItem(int position) throws NotReadyException;

    void notifyAllSelectionChanges();

    void notifyItemChanged(int position);

    void notifyItemRangeChanged(int positionStart, int itemCount);

    void syncSelectionList();

    void syncSelectionList(List<T> itemList);
}
