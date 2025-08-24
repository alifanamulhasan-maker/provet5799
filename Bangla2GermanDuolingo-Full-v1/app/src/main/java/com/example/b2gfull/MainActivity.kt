
package com.example.b2gfull

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Locale

class MainActivity : ComponentActivity() {
  private var tts: TextToSpeech? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    tts = TextToSpeech(this) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.GERMAN
        val male = tts?.voices?.firstOrNull { v -> v.locale.language=="de" && v.name.lowercase().contains("male") }
        if (male!=null) tts?.voice = male
      }
    }
    setContent { MaterialTheme { AppRoot(tts) } }
  }
  override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

sealed class Screen { object Home: Screen(); object Category: Screen(); object Lesson: Screen(); object Review: Screen(); object Curriculum: Screen(); object B1Exam: Screen(); object ExamReport: Screen() }

@kotlinx.serialization.Serializable data class Lesson(val id:String,val title:String,val level:String,val steps:List<Step>)
@kotlinx.serialization.Serializable data class Step(val type:String,val promptBn:String,val promptDe:String?=null,val options:List<String>?=null,val answer:String?=null,val pairs:List<PairItem>?=null)
@kotlinx.serialization.Serializable data class PairItem(val bn:String,val de:String)
@kotlinx.serialization.Serializable data class Category(val level:String,val lessons:List<Lesson>)

@kotlinx.serialization.Serializable data class CurriculumStage(val stage:String, val goals:List<String>, val sampleTopics:List<String>, val checkpointLessonIds:List<String>)

@kotlinx.serialization.Serializable data class B1Exam(val reading: List<B1Q>, val listening: List<B1Q>, val writing: List<B1Task>, val speaking: List<B1Task>)
@kotlinx.serialization.Serializable data class B1Q(val prompt:String, val options: List<String>, val answer: String)
@kotlinx.serialization.Serializable data class B1Task(val prompt:String, val keywords: List<String>, val minWords:Int)

@Composable
fun AppRoot(tts: TextToSpeech?) {
  val ctx = LocalContext.current
  val prefs = ctx.getSharedPreferences("b2g_full_prefs", Context.MODE_PRIVATE)
  var screen by remember { mutableStateOf<Screen>(Screen.Home) }
  var selected by remember { mutableStateOf<Category?>(null) }
  var lesson by remember { mutableStateOf<Lesson?>(null) }
  var examResult by remember { mutableStateOf<String?>(null) }

  when (screen) {
    is Screen.Home -> HomeScreen(
      prefs = prefs,
      onOpen = { c -> selected = c; screen = Screen.Category },
      onOpenReview = { screen = Screen.Review },
      onOpenCurriculum = { screen = Screen.Curriculum },
      onOpenB1 = { screen = Screen.B1Exam }
    )
    is Screen.Category -> CategoryScreen(category = selected, onBack = { screen = Screen.Home }, onOpenLesson = { l -> lesson = l; screen = Screen.Lesson })
    is Screen.Lesson -> LessonScreen(lesson!!, onBack = { screen = Screen.Category }, tts = tts, prefs = prefs)
    is Screen.Review -> ReviewScreen(prefs = prefs, onBack = { screen = Screen.Home })
    is Screen.Curriculum -> CurriculumScreen(onBack = { screen = Screen.Home })
    is Screen.B1Exam -> B1ExamScreen(prefs = prefs, tts = tts, onDone = { report -> examResult = report; screen = Screen.ExamReport })
    is Screen.ExamReport -> ExamReportScreen(report = examResult ?: "No report", onBack = { screen = Screen.Home })
  }
}

@Composable
fun HomeScreen(prefs: SharedPreferences, onOpen:(Category)->Unit, onOpenReview:()->Unit, onOpenCurriculum:()->Unit, onOpenB1:()->Unit) {
  val ctx = LocalContext.current
  val json = ctx.assets.open("b2g_lessons_full.json").bufferedReader().use{it.readText()}
  val data = Json{ ignoreUnknownKeys=true }.decodeFromString<List<Category>>(json)
  val xp = prefs.getInt("xp",0)
  val streak = prefs.getInt("streak",0)
  Scaffold(topBar={ TopAppBar(title={ Text("B2G Full • XP: $xp • 🔥 $streak") })}){pv->
    Column(Modifier.padding(pv).padding(12.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = onOpenReview) { Text("🔁 Review") }
        Button(onClick = onOpenCurriculum) { Text("📚 Curriculum") }
        Button(onClick = onOpenB1) { Text("🎓 B1 Mock Exam") }
      }
      LazyColumn {
        items(data){cat->
          Card(Modifier.padding(12.dp).fillMaxWidth().clickable{ onOpen(cat) }){
            Column(Modifier.padding(16.dp)) {
              Text("Level: ${cat.level}", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold)
              Text("Lessons: ${cat.lessons.size}")
            }
          }
        }
      }
    }
  }
}

// The rest of composables and helper functions are identical to the lite version provided earlier:
// CurriculumScreen, B1ExamScreen, ExamReportScreen, evaluateText(), SpeakingPanel(), CategoryScreen(),
// LessonScreen(), nextOrFinish(), addToReview(), ReviewScreen(), ReviewItem(), removeFromPool(), promoteInPool(),
// McqStep(), ListenStep(), MatchStep(), TypeInStep(), SpeakStep(), startListening().
// For brevity, you can paste the same implementations from the lite project and it will compile.
