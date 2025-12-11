package tw.com.rehome.ui.view

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.squareup.moshi.Moshi
import tw.com.rehome.model.Survey
import tw.com.rehome.model.SurveyJsonAdapter

@Composable
internal fun SurveyView() {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(state = rememberScrollState())
    ) {

        val survey = readJsonFromAssets(context = LocalContext.current, fileName = "survey.json")
        val surveyObj = SurveyJsonAdapter(Moshi.Builder().build()).fromJson(survey)

        surveyObj?.questions?.forEach {
            val title = "${it.id}.${it.question}"
            if (!it.options.isNullOrEmpty()) SingleSelect(question = title, options = it.options)
            else SlideSelect(question = title)
        }

        Button(onClick = {
            // TODO : Next page
        }) {
            Text("submit")
        }
    }
}

private fun readJsonFromAssets(context: Context, fileName: String): String {
    return context.assets.open(fileName).bufferedReader().use { it.readText() }
}

@Composable
private fun SingleSelect(question: String?, options: List<String?>) {
    Column {
        if (question != null) Text(text = question)
        var selectedOption: String? by remember { mutableStateOf(null) }
        SingleSelectionList(options = options.filterNotNull(), selectedOption = selectedOption) {
            selectedOption = it
        }
    }
}

@Composable
private fun SlideSelect(question: String?) {
    Column {
        if (question != null) Text(text = question)
        RatingSlider()
    }
}

@Composable
fun SingleSelectionList(
    options: List<String>,
    selectedOption: String? = null,
    onSelectionChange: (String) -> Unit
) {
    var selected by remember { mutableStateOf(selectedOption) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selected = option
                        onSelectionChange(option)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selected),
                    onClick = {
                        selected = option
                        onSelectionChange(option)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun RatingSlider() {
    var sliderValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 0f..5f,
            steps = 4, // 中間有 4 個間隔點 (1, 2, 3, 4)
            modifier = Modifier.fillMaxWidth()
        )

        // 顯示刻度標籤
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "X",
                style = MaterialTheme.typography.bodySmall,
                color = if (sliderValue == 0f) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            for (i in 1..5) {
                Text(
                    text = i.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sliderValue.toInt() == i) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SingleSelectPreview() {
    SingleSelect(question = "test", options = listOf("1", "2", "3"))
    SingleSelect(question = "test2", options = listOf("1", "2", "3"))
    SingleSelect(question = "test3", options = listOf("1", "2", "3"))
}

@Preview(showBackground = true)
@Composable
private fun SlideSelectPreview() {
    SlideSelect(question = "test")
}