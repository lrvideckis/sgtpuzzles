package name.boyle.chris.sgtpuzzles.config

import android.content.Context
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.plusAssign
import name.boyle.chris.sgtpuzzles.R
import name.boyle.chris.sgtpuzzles.Utils.listFromSeparated
import name.boyle.chris.sgtpuzzles.backend.BRIDGES
import name.boyle.chris.sgtpuzzles.backend.BackendName
import name.boyle.chris.sgtpuzzles.backend.GUESS
import name.boyle.chris.sgtpuzzles.backend.GameEngine
import name.boyle.chris.sgtpuzzles.backend.MAP
import name.boyle.chris.sgtpuzzles.backend.SIGNPOST
import name.boyle.chris.sgtpuzzles.backend.SINGLES
import name.boyle.chris.sgtpuzzles.config.PrefsConstants.CATEGORY_THIS_GAME
import name.boyle.chris.sgtpuzzles.config.PrefsConstants.CATEGORY_THIS_GAME_DISPLAY_AND_INPUT

class ConfigPreferencesBuilder(
    private val category: PreferenceCategory,
    private val context: Context,
    private val gameEngine: GameEngine,
    private val backend: BackendName
) : ConfigBuilder {

    /** Ignores the title for the preferences screen as there's nowhere to put it. */
    override fun setTitle(title: String) {}

    /** Does nothing as the preferences screen is already shown. */
    override fun dialogShow() {}

    private fun isInThisCategory(kw: String) = (CATEGORIES[kw] ?: CATEGORY_THIS_GAME) == category.key

    override fun addString(whichEvent: Int, kw: String, name: String, value: String) {
        if (!isInThisCategory(kw)) return
        category += EditTextPreference(context).withCommonProps(kw, name).apply {
            text = value
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newVal ->
                gameEngine.configSetString(name, newVal.toString(), true)
                gameEngine.savePrefs(context)
                true
            }
        }
        addNote(kw)
    }

    override fun addBoolean(whichEvent: Int, kw: String, name: String, checked: Boolean) {
        if (!isInThisCategory(kw)) return
        category += SwitchPreferenceCompat(context).withCommonProps(kw, name).apply {
            isChecked = checked
            setOnPreferenceChangeListener { _, newVal ->
                gameEngine.configSetBool(name, newVal as Boolean, true)
                gameEngine.savePrefs(context)
                true
            }
        }
        addNote(kw)
    }

    override fun addChoices(
        whichEvent: Int,
        kw: String,
        name: String,
        choiceList: String,
        choiceKWList: String,
        selection: Int
    ) {
        if (!isInThisCategory(kw)) return
        val choices = listFromSeparated(choiceList).toTypedArray()
        val choiceKWs = listFromSeparated(choiceKWList).toTypedArray()
        category += ListPreference(context).withCommonProps(kw, name).apply {
            entries = choices
            entryValues = choiceKWs
            value = choiceKWs[selection]
            dialogTitle = name
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newVal ->
                gameEngine.configSetChoice(name, entryValues.indexOf(newVal), true)
                gameEngine.savePrefs(context)
                true
            }
        }
        addNote(kw)
    }

    private fun addNote(kw: String) {
        NOTES[backend to kw]?.let {
            category += Preference(context).withBasicProps().apply {
                setSummary(it)
                isSelectable = false
            }
        }
    }

    private var orderCounter = 0

    private fun <T : Preference> T.withBasicProps(): T =
        apply {
            order = orderCounter++
            isIconSpaceReserved = false
            isPersistent = false
        }

    private fun <T : Preference> T.withCommonProps(kw: String, name: String): T =
        withBasicProps().apply {
            key = kw
            title = name
        }

    companion object {

        private val CATEGORIES = mapOf(
            "one-key-shortcuts" to CATEGORY_THIS_GAME_DISPLAY_AND_INPUT,
        )

        private val NOTES = mapOf(
            BRIDGES to "show-hints" to R.string.enableButtonNote,
            GUESS to "show-labels" to R.string.enableButtonNote,
            MAP to "show-labels" to R.string.enableButtonsNote,
            MAP to "flash-type" to R.string.flashTypeNote,
            SIGNPOST to "flash-type" to R.string.flashTypeNote,
            SINGLES to "show-black-nums" to R.string.enableTapOutsideNote,
        )

    }
}