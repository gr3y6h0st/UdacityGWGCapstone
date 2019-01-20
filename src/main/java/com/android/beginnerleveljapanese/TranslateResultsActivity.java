package com.android.beginnerleveljapanese;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.android.beginnerleveljapanese.MainActivity.ID_TRANSLATOR_LOADER;
import static com.android.beginnerleveljapanese.cloud.Translator.translate;

public class TranslateResultsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    @BindView(R.id.translate_test_tv) TextView translatedTextTv;
    @BindView(R.id.translate_original_tv) TextView originalTextTv;
    @BindView(R.id.translate_activity_fab) FloatingActionButton translateSearchFab;
    private Bundle translateBundle = new Bundle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate_results);
        ButterKnife.bind(this);
        onSearchRequested();
        setupActionBar();
        handleIntent(getIntent());

        translateSearchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearchRequested();
            }
        });
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     *  Per Android Documentation on Search Dialog: handleIntent method handles SingleTop Searchable
     *  Activities to avoid multiple instances of itself when users conduct multiple searches.
     */

    private void handleIntent(Intent intent) {
        if(Intent.ACTION_SEARCH.equals(intent.getAction())){
            String searchQuery = intent.getStringExtra(SearchManager.QUERY);
            translateBundle.putString("textToTranslate", searchQuery);
            originalTextTv.setText(searchQuery);
            getSupportLoaderManager().restartLoader(ID_TRANSLATOR_LOADER, translateBundle, this);
            System.out.println("LOADING....");
        }
    }

    @SuppressLint("StaticFieldLeak")
    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable final Bundle args) {
        //AsyncTaskLoader calls on Translator method to translate desired text.

        return new AsyncTaskLoader<String>(this) {
            /**
             * mTranslator variable holds the string data throughout app lifecycle to prevent
             * constant translate async background calls when users  rotate/place app in background.
             */
            String mTranslatorData;

            @Override
            protected void onStartLoading() {
                if (args == null) {
                    System.out.println(" ARGS NULL!");
                    return;
                }
                if (mTranslatorData != null) {
                    deliverResult(mTranslatorData);
                    System.out.println("Translator data isn't null, delivering data!");
                } else{
                    forceLoad();
                    System.out.println("forceloading.");
                }
            }

            @Override
            public String loadInBackground() {
                String translateStart = args.getString("textToTranslate");
                System.out.println(translateStart);
                try{
                    //where background thread runs Translator method.
                   return translate(translateStart);

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(@Nullable String data) {
                System.out.println("Deliver Result: " + data);
                mTranslatorData = data;
                super.deliverResult(data);
            }

        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String translatedText) {
        if (translatedText != null) {
            Log.v("LOADER FINISHED with: ", translatedText);
            translatedTextTv.setText(translatedText);
            //getSupportLoaderManager().destroyLoader(ID_TRANSLATOR_LOADER);
        } else {
            Log.v("LOADER FINISHED: ", "EMPTY");

        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if( id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }

        if (id == R.id.action_settings) {
            Intent settingsActivityIntent = new Intent(TranslateResultsActivity.this, SettingsActivity.class);
            startActivity(settingsActivityIntent);
            return true;
        } else if (id == R.id.menu_search_icon){
            //Translator Search
            onSearchRequested();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
