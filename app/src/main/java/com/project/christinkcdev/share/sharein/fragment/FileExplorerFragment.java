package com.project.christinkcdev.share.sharein.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.android.framework.io.DocumentFile;
import com.google.android.material.snackbar.Snackbar;
import com.project.christinkcdev.share.sharein.R;
import com.project.christinkcdev.share.sharein.adapter.PathResolverRecyclerAdapter;
import com.project.christinkcdev.share.sharein.app.Activity;
import com.project.christinkcdev.share.sharein.dialog.FolderCreationDialog;
import com.project.christinkcdev.share.sharein.ui.callback.DetachListener;
import com.project.christinkcdev.share.sharein.ui.callback.IconSupport;
import com.project.christinkcdev.share.sharein.ui.callback.TitleSupport;

import java.io.File;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileExplorerFragment} factory method to
 * create an instance of this fragment.
 */
public class FileExplorerFragment  extends FileListFragment implements Activity.OnBackPressedListener, DetachListener, IconSupport, TitleSupport {

    public static final String TAG = FileExplorerFragment.class.getSimpleName();

    private RecyclerView mPathView;
    private FilePathResolverRecyclerAdapter mPathAdapter;
    private DocumentFile mRequestedPath = null;

    public static DocumentFile getReadableFolder(DocumentFile documentFile)
    {
        DocumentFile parent = documentFile.getParentFile();

        if (parent == null)
            return null;

        return parent.canRead()
                ? parent
                : getReadableFolder(parent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setDividerView(R.id.fragment_fileexplorer_separator);
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        View adaptedView = getLayoutInflater().inflate(R.layout.layout_file_explorer, null, false);
        listViewContainer.addView(adaptedView);

        mPathView = adaptedView.findViewById(R.id.fragment_fileexplorer_pathresolver);
        mPathAdapter = new FilePathResolverRecyclerAdapter(getContext());

        mPathAdapter.setOnClickListener(holder -> goPath(holder.index.object));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        layoutManager.setStackFromEnd(true);

        mPathView.setLayoutManager(layoutManager);
        mPathView.setHasFixedSize(true);
        mPathView.setAdapter(mPathAdapter);

        return super.onListView(mainContainer, (ViewGroup) adaptedView.findViewById(R.id.fragment_fileexplorer_listViewContainer));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (mRequestedPath != null)
            requestPath(mRequestedPath);
    }

    @Override
    public boolean onBackPressed()
    {
        DocumentFile path = getAdapter().getPath();

        if (path == null)
            return false;

        DocumentFile parentFile = getReadableFolder(path);

        if (parentFile == null || File.separator.equals(parentFile.getName()))
            goPath(null);
        else
            goPath(parentFile);

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_file_explorer, menu);
    }

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        mPathAdapter.goTo(getAdapter().getPath());
        mPathAdapter.notifyDataSetChanged();

        if (mPathAdapter.getItemCount() > 0)
            mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.actions_file_explorer_create_folder) {
            if (getAdapter().getPath() != null && getAdapter().getPath().canWrite())
                new FolderCreationDialog(getContext(), getAdapter().getPath(), directoryFile -> refreshList()).show();
            else
                Snackbar.make(getListView(), R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_folder_white_24dp;
    }

    public PathResolverRecyclerAdapter getPathAdapter()
    {
        return mPathAdapter;
    }

    public RecyclerView getPathView()
    {
        return mPathView;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_fileExplorer);
    }

    public void requestPath(DocumentFile file)
    {
        if (!isAdded()) {
            mRequestedPath = file;
            return;
        }

        mRequestedPath = null;

        goPath(file);
    }

    private class FilePathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<DocumentFile>
    {
        public FilePathResolverRecyclerAdapter(Context context)
        {
            super(context);
        }

        @Override
        public Holder.Index<DocumentFile> onFirstItem()
        {
            return new Holder.Index<>(getContext().getString(R.string.text_home), R.drawable.ic_home_white_24dp, null);
        }

        public void goTo(DocumentFile file)
        {
            ArrayList<Holder.Index<DocumentFile>> pathIndex = new ArrayList<>();
            DocumentFile currentFile = file;

            while (currentFile != null) {
                Holder.Index<DocumentFile> index = new Holder.Index<>(currentFile.getName(), currentFile);

                pathIndex.add(index);

                currentFile = currentFile.getParentFile();

                if (currentFile == null && ".".equals(index.title))
                    index.title = getString(R.string.text_fileRoot);
            }

            initAdapter();

            synchronized (getList()) {
                while (pathIndex.size() != 0) {
                    int currentStage = pathIndex.size() - 1;

                    getList().add(pathIndex.get(currentStage));
                    pathIndex.remove(currentStage);
                }
            }
        }
    }
}