package tw.com.rehome.ui.view

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squareup.moshi.Moshi
import tw.com.rehome.model.SurveyJsonAdapter

@Composable
internal fun SurveyView(
    goToResult: () -> Unit
) {
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .verticalScroll(state = rememberScrollState())
    ) {

        Text(
            "探索你的療癒旅程",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "誠實回答每個問題，讓我們更了解你的內在需求",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        val survey = readJsonFromAssets(context = LocalContext.current, fileName = "survey.json")
        val surveyObj = SurveyJsonAdapter(Moshi.Builder().build()).fromJson(survey)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            surveyObj?.questions?.forEach {
                val title = "${it.id}.${it.question}"
                if (!it.options.isNullOrEmpty()) SingleSelect(
                    question = title,
                    options = it.options
                )
                else SlideSelect(question = title)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = goToResult) {
                Text("submit")
            }
        }
    }
}

private fun readJsonFromAssets(context: Context, fileName: String): String {
    return context.assets.open(fileName).bufferedReader().use { it.readText() }
}

@Composable
private fun SingleSelect(question: String?, options: List<String?>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (question != null) Text(text = question)
            var selectedOption: String? by remember { mutableStateOf(null) }
            SingleSelectionList(
                options = options.filterNotNull(),
                selectedOption = selectedOption
            ) {
                selectedOption = it
            }
        }
    }
}

@Composable
private fun SlideSelect(question: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (question != null) Text(text = question)
            RatingSlider()
        }
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