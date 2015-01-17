package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.BookmarkAdapter;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.Bookmark;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.service.Controls;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.MaterialCompatThemer;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkDialog extends DialogFragment {

    private BookmarkAdapter adapter;
    private AlertDialog dialog;

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_bookmark, null);

        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final StateManager stateManager = StateManager.getInstance(getActivity());
        final BookDetail book = stateManager.getBook();
        final ArrayList<Bookmark> allBookmarks = db.getAllBookmarks(book.getId());

        BookmarkAdapter.OnOptionsMenuClickedListener listener = new BookmarkAdapter.OnOptionsMenuClickedListener() {
            @Override
            public void onOptionsMenuClicked(final int position, View v) {
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        switch (item.getItemId()) {
                            case R.id.edit_book:
                                final Bookmark editBookmark = adapter.getItem(position);
                                builder.setTitle(R.string.bookmark_edit_title);
                                final EditText editText = new EditText(getActivity());
                                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                                int padding = getResources().getDimensionPixelSize(R.dimen.horizontal_margin);
                                editText.setPadding(padding, padding, padding, padding);
                                builder.setView(editText);
                                editText.setText(editBookmark.getTitle());
                                builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        editBookmark.setTitle(editText.getText().toString());
                                        db.updateBookmark(editBookmark);
                                        adapter.notifyItemChanged(position);
                                    }
                                });
                                builder.setNegativeButton(R.string.abort, null);
                                AlertDialog dialog = builder.show();
                                MaterialCompatThemer.theme(dialog);
                                final Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                                positive.setEnabled(editText.getText().toString().length() > 0);
                                editText.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                    }

                                    @Override
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        positive.setEnabled(s.length() > 0);
                                    }

                                    @Override
                                    public void afterTextChanged(Editable s) {

                                    }
                                });
                                return true;
                            case R.id.delete_book:
                                final Bookmark deleteBookmark = adapter.getItem(position);
                                builder.setTitle(R.string.bookmark_delete_title);
                                builder.setMessage(deleteBookmark.getTitle());
                                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        db.deleteBookmark(deleteBookmark);
                                        adapter.removeItem(position);
                                    }
                                });
                                builder.setNegativeButton(R.string.delete_book_keep, null);
                                MaterialCompatThemer.theme(builder.show());
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }

            @Override
            public void onBookmarkClicked(int position) {
                Bookmark bookmark = adapter.getItem(position);
                Controls controls = new Controls(getActivity());
                controls.changeBookPosition(bookmark.getMediaId(), bookmark.getPosition());
                dialog.cancel();
            }
        };

        final RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        adapter = new BookmarkAdapter(getActivity(), allBookmarks, listener);
        recyclerView.setAdapter(adapter);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);


        ImageView addButton = (ImageView) v.findViewById(R.id.add);
        final EditText bookmarkTitle = (EditText) v.findViewById(R.id.edit1);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bookmark bookMark = new Bookmark();

                long bookId = book.getId();
                ArrayList<MediaDetail> media = stateManager.getMedia();
                long mediaId = book.getCurrentMediaId();
                int position = book.getCurrentMediaPosition();
                String mediaName = "";
                for (int i = 0; i < media.size(); i++) {
                    if (media.get(i).getId() == mediaId) {
                        mediaName = media.get(i).getName();
                    }
                }

                String title = bookmarkTitle.getText().toString();
                if (title == null || title.equals("")) {
                    title = mediaName;
                }

                bookMark.setTitle(title);
                bookMark.setMediaId(mediaId);
                bookMark.setBookId(bookId);
                bookMark.setPosition(position);

                boolean alreadyContaining = allBookmarks.contains(bookMark);
                if (alreadyContaining) {
                    Toast.makeText(getActivity(), R.string.bookmark_exists, Toast.LENGTH_SHORT).show();
                } else {
                    long id = db.addBookmark(bookMark);
                    bookMark.setId(id);
                    int index = adapter.addItem(bookMark);
                    recyclerView.smoothScrollToPosition(index);
                }
            }
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);
        builder.setTitle(R.string.bookmark);
        builder.setNegativeButton(R.string.abort, null);
        dialog = builder.create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialCompatThemer.theme(getDialog());
    }
}