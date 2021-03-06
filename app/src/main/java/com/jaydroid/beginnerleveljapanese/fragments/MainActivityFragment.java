package com.jaydroid.beginnerleveljapanese.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.jaydroid.beginnerleveljapanese.activities.MainActivity;
import com.jaydroid.beginnerleveljapanese.activities.PhrasesCategoryActivity;
import com.jaydroid.beginnerleveljapanese.R;
import com.jaydroid.beginnerleveljapanese.data.FavoriteData;
import com.jaydroid.beginnerleveljapanese.data.FavoriteDbHelper;
import com.jaydroid.beginnerleveljapanese.data.FavoritesContract;
import com.jaydroid.beginnerleveljapanese.data.HiraganaData;
import com.jaydroid.beginnerleveljapanese.utils.FavoritedPhrasesAdapter;
import com.jaydroid.beginnerleveljapanese.utils.HiraganaAdapter;
import com.jaydroid.beginnerleveljapanese.utils.PhrasesAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.jaydroid.beginnerleveljapanese.utils.HiraganaDataUtils.getHiraganaData;

public class MainActivityFragment extends Fragment implements
        PhrasesAdapter.PhrasesAdapterOnClickListener,
        FavoritedPhrasesAdapter.favoritedPhrasesAdapterOnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        HiraganaAdapter.HiraganaAdapterOnClickListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    public static final String PHRASE_CATEGORY_SELECTED = "category_selected";
    public static final String PHRASE_ROMAJI_TRANSLATION = "Romaji";
    public static final String PHRASE_STRING_ARRAY = "phrases_array";
    public static final String PHRASE_ERROR_CHECK = "error_message";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int ID_FAVORITES_LOADER = 1990;

    @BindView(R.id.fragment_main_rv)
    RecyclerView fragment_main_rv;

    private RecyclerView.LayoutManager phrasesLayoutManager;
    private RecyclerView.LayoutManager favoritesLayoutManager;
    private RecyclerView.LayoutManager hiraganaLayoutManager;

    private PhrasesAdapter phrasesAdapter;
    private FavoritedPhrasesAdapter favoritedPhrasesAdapter;
    private HiraganaAdapter hiraganaAdapter;

    private String[] phrase_label_arr;
    private int mCurrentSection;

    private ArrayList<FavoriteData> favoriteDataArrayList = new ArrayList<>();
    private List<HiraganaData> hiraganaDataArrayList = new ArrayList<>();
    private ArrayList<String> favoriteStatus = new ArrayList<>();
    private FavoriteData favoriteData;
    private String romaji_text;
    private String english_text;
    private Boolean fav_bool;
    private Cursor mCursor;

    public static final String[] FAVORITES_PROJECTION = {
            FavoritesContract.FavoriteEntry.COLUMN_ENGLISH_TEXT,
            FavoritesContract.FavoriteEntry.COLUMN_ROMAJI_TEXT,
            FavoritesContract.FavoriteEntry.COLUMN_FAVORITE_BOOL,
            FavoritesContract.FavoriteEntry.COLUMN_PHRASE_CATEGORY

    };
    public static final int INDEX_ENG_TEXT = 0;
    public static final int INDEX_ROMAJI_TEXT = 1;
    public static final int INDEX_BOOL_VALUE = 2;
    public static final int INDEX_PHRASE_CATEGORY = 3;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MainActivityFragment newInstance(int sectionNumber) {
        MainActivityFragment fragment = new MainActivityFragment();
        switch (sectionNumber) {
            default:
                Bundle phrases_bundle = new Bundle();
                phrases_bundle.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(phrases_bundle);
                break;
        }
        return fragment;
    }

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, rootView);

        if (getArguments() != null) {
            mCurrentSection = getArguments().getInt(ARG_SECTION_NUMBER);
        } /*else {
            //Log.v(TAG, "Check Arguments Bundle from MainActivity; it may be null.");
        }*/

        switch(mCurrentSection){
            case 1:
                int columnCount = 2;
                phrase_label_arr = getResources().getStringArray(R.array.phrase_topic_labels);
                phrasesAdapter = new PhrasesAdapter(getContext(), phrase_label_arr,this);

                phrasesLayoutManager = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
                //prevents RecyclerView from calculating gaps between items, causing shifts in the layout.
                ((StaggeredGridLayoutManager) phrasesLayoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);

                fragment_main_rv.setLayoutManager(phrasesLayoutManager);
                fragment_main_rv.setHasFixedSize(true);
                fragment_main_rv.setAdapter(phrasesAdapter);
                fragment_main_rv.setItemAnimator(null);
                break;

            case 2:
                FavoriteDbHelper favoriteDbHelper = new FavoriteDbHelper(getActivity().getApplicationContext());

                //initialize adapter
                favoritedPhrasesAdapter = new FavoritedPhrasesAdapter(getContext(), favoriteDataArrayList, this);

                //set layout manager
                favoritesLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
                fragment_main_rv.setLayoutManager(favoritesLayoutManager);
                fragment_main_rv.setHasFixedSize(true);

                //set adapter.
                fragment_main_rv.setAdapter(favoritedPhrasesAdapter);

                if(mCursor != null){
                    //query the favorites via Cursor
                    displayFavoritePhrases(mCursor);
                } else {
                    //Log.i(TAG, "CURSOR WAS NULL, INITIATING LOADER.");
                    //initiate Loader to populate Cursor
                    getLoaderManager().initLoader(ID_FAVORITES_LOADER, null, this);
                }

                break;

            case 3:

                String[] hiragana_data_arr = getResources().getStringArray(R.array.hiragana_syllabary);
                String[] hirgana_romaji_arr = getResources().getStringArray(R.array.hiragana_syllabary_english_translate);
                hiraganaDataArrayList = getHiraganaData(getActivity().getApplicationContext(), hiragana_data_arr, hirgana_romaji_arr);

                //Log.i(TAG, "TEST FOR HIRAGANA: " + hiragana_data_arr[0] + hiragana_data_arr[1]);

                hiraganaAdapter = new HiraganaAdapter(getContext(), hiraganaDataArrayList,this);
                hiraganaLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(),5, RecyclerView.VERTICAL,false);
                fragment_main_rv.setLayoutManager(hiraganaLayoutManager);
                fragment_main_rv.setHasFixedSize(true);
                fragment_main_rv.setAdapter(hiraganaAdapter);

                break;
        }
        return rootView;
    }

    private void displayFavoritePhrases(Cursor c) {

        try {
            while (c.moveToNext()) {
                english_text = c.getString(c.getColumnIndex(FavoritesContract.FavoriteEntry.COLUMN_ENGLISH_TEXT));
                romaji_text = c.getString(c.getColumnIndex(FavoritesContract.FavoriteEntry.COLUMN_ROMAJI_TEXT));
                fav_bool = Boolean.parseBoolean(c.getString(c.getColumnIndex(FavoritesContract.FavoriteEntry.COLUMN_FAVORITE_BOOL)));

                favoriteData = new FavoriteData(
                        english_text,
                        romaji_text,
                        fav_bool
                );

                //populate ArrayList
                favoriteDataArrayList.add(favoriteData);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        /*finally {
            //Log.v(TAG, "Calling displayFavoritePhrases: " + fav_bool);
        }*/
        favoritedPhrasesAdapter.notifyPhrasesDataChange(favoriteDataArrayList);
    }

    @Override
    public void onPhraseCategoryClicked(int clickedPosition) {
        String[] phraseCategoryArray = getResources().getStringArray(R.array.phrase_topic_labels);
        String phraseCategorySelected = phraseCategoryArray[clickedPosition];
        Bundle phrasesCategoryBundle = new Bundle();
        phrasesCategoryBundle.putString(PHRASE_CATEGORY_SELECTED, phraseCategorySelected);

        switch(clickedPosition) {

            case 0:
                String[] greetings_phrases = getResources().getStringArray(R.array.greetings_phrases);
                String[] greetings_romaji_phrases = getResources().getStringArray(R.array.greetings_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, greetings_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, greetings_romaji_phrases);

                break;

            case 1:
                String [] common_phrases = getResources().getStringArray(R.array.common_phrases);
                String [] common_romaji_phrases = getResources().getStringArray(R.array.common_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, common_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, common_romaji_phrases);

                break;

            case 2:
                String [] numbers_phrases = getResources().getStringArray(R.array.numbers_phrases);
                String [] numbers_romaji_phrases = getResources().getStringArray(R.array.numbers_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, numbers_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, numbers_romaji_phrases);

                break;

            case 3:
                String [] emergency_phrases = getResources().getStringArray(R.array.emergency_phrases);
                String [] emergency_romaji_phrases = getResources().getStringArray(R.array.emergency_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, emergency_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, emergency_romaji_phrases);

                break;

            case 4:
                String [] shopping_vocab = getResources().getStringArray(R.array.shopping_phrases_vocab);
                String [] shopping_romaji_vocab = getResources().getStringArray(R.array.shopping_phrases_vocab_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, shopping_vocab);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, shopping_romaji_vocab);

                break;

            case 5:
                String [] eating_phrases = getResources().getStringArray(R.array.eating_phrases);
                String [] eating_phrases_romaji = getResources().getStringArray(R.array.eating_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, eating_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, eating_phrases_romaji);

                break;
            case 6:
                String [] accommodation_phrases = getResources().getStringArray(R.array.accommodation_phrases);
                String [] accommodation_phrases_romaji = getResources().getStringArray(R.array.accommodation_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, accommodation_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, accommodation_phrases_romaji);

                break;
            case 7:
                String [] weather_phrases = getResources().getStringArray(R.array.weather_phrases);
                String [] weather_phrases_romaji = getResources().getStringArray(R.array.weather_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, weather_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, weather_phrases_romaji);

                break;

            case 8:
                String [] places_phrases = getResources().getStringArray(R.array.places_phrases);
                String [] places_phrases_romaji = getResources().getStringArray(R.array.places_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, places_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, places_phrases_romaji);

                break;

            case 9:
                String [] transportation_phrases = getResources().getStringArray(R.array.transportation_phrases);
                String [] transportation_phrases_romaji = getResources().getStringArray(R.array.transportation_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, transportation_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, transportation_phrases_romaji);

                break;

            case 10:
                String [] time_phrases = getResources().getStringArray(R.array.time_phrases);
                String [] time_phrases_romaji = getResources().getStringArray(R.array.time_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, time_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, time_phrases_romaji);

                break;

            case 11:
                String [] colors_phrases = getResources().getStringArray(R.array.color_phrases);
                String [] colors_phrases_romaji = getResources().getStringArray(R.array.color_phrases_romaji);
                phrasesCategoryBundle.putStringArray(PHRASE_STRING_ARRAY, colors_phrases);
                phrasesCategoryBundle.putStringArray(PHRASE_ROMAJI_TRANSLATION, colors_phrases_romaji);

                break;

            default:
                phrasesCategoryBundle.putString(PHRASE_ERROR_CHECK, "Haven't created a " +
                        "String Array for this section quite yet." );
        }
        final Intent phraseCategoryIntent = new Intent(Objects.requireNonNull(getActivity()).getApplicationContext(), PhrasesCategoryActivity.class);
        //load intent extras in bundle
        phraseCategoryIntent.putExtras(phrasesCategoryBundle);
        //Start Activity
        startActivity(phraseCategoryIntent);
    }

    @Override
    public void onPhraseClicked(int clickedPosition) {
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {

        if (loaderId == ID_FAVORITES_LOADER) {
            //Uri to query all data for category
            Uri favoritesQueryUri = FavoritesContract.FavoriteEntry.CONTENT_URI;
            return new CursorLoader(Objects.requireNonNull(getContext()),
                    favoritesQueryUri,
                    PhrasesCategoryActivity.PHRASES_CATEGORY_PROJECTION,
                    FavoritesContract.FavoriteEntry.COLUMN_FAVORITE_BOOL + " = ?",
                    new String[]{"true"},
                    null);
        }
        throw new RuntimeException("Loader not implemented: " + loaderId);
    }
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mCursor = data;

        if(data != null) {
            displayFavoritePhrases(mCursor);
        }
    }
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
    @Override
    public void onHiraganaSymbolClicked(int clickedPosition) {
        //do nothing for now
    }
}
