package com.example.mobilesoftwareproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mobilesoftwareproject.data.QuizData
import com.example.mobilesoftwareproject.data.RankingStore
import com.example.mobilesoftwareproject.data.WrongAnswerStore
import com.example.mobilesoftwareproject.model.Ranking
import com.example.mobilesoftwareproject.model.WrongAnswer
import com.example.mobilesoftwareproject.navigation.Screen
import com.example.mobilesoftwareproject.ui.theme.CategoryScreen
import com.example.mobilesoftwareproject.ui.theme.MobileSoftWareProjectTheme
import com.example.mobilesoftwareproject.ui.theme.QuizScreen
import com.example.mobilesoftwareproject.ui.theme.RankingScreen
import com.example.mobilesoftwareproject.ui.theme.ResultScreen
import com.example.mobilesoftwareproject.ui.theme.WrongNoteScreen
import com.example.mobilesoftwareproject.ui.theme.SoundManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this
        WrongAnswerStore.clear()
        RankingStore.clearAll(context)
        enableEdgeToEdge()
        setContent {
            MobileSoftWareProjectTheme {
                QuizNavHost()
            }
        }
    }
    // 앱이 종료될 때 SoundPool 자원을 해제
    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

@Composable
fun QuizNavHost() {
    val navController = rememberNavController() //화면 전환을 수행하는 컨트롤러
    NavHost( //어떤 route에 어떤 화면을 연결할 것인가
        navController = navController,
        startDestination = Screen.Category.route //첫 화면을 카테고리화면으로
    ) {
        //현재 화면의 위치 설정 - 카테고리 화면
        composable(route = Screen.Category.route) {
            CategoryScreen( //얘는 버튼 누르면 신호를 보내주는 역할
                //퀴즈 시작버튼을 누를 시 작동
                onStartQuiz = { categoryId ->
                    navController.navigate( //해당 위치로 이동 ( 카테고리 화면 -> 퀴즈 화면 )
                        Screen.Quiz.createRoute(categoryId)
                    )
                },
                //랭킹 버튼을 누를 시 작동
                onShowRanking = { categoryId ->
                    navController.navigate( // 카테고리 화면 -> 랭킹 화면
                        Screen.Ranking.createRoute(categoryId)
                    )
                },
                //오답 노트 버튼을 누를 시 작동
                onShowWrongNote = { categoryId ->   //
                    navController.navigate( // 카테고리 화면 -> 오답노트 화면
                        route = Screen.WrongNote.createRoute(categoryId) // 🔧 createRoute 사용
                    )
                },
            )
        }

        //카테고리별 랭킹 화면
        composable( //해당 화면으로 오기위해선, route와 어떤 카테고리인지 나타내는 categoryId 필요
            route = Screen.Ranking.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType } // 인자가 있을경우,네비게이션에게 타입을 알려줘야함
            )
        ) { backStackEntry -> // 보통 route에서 저장된 값들을 가져오기위함
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "" //route에서 {categoryId}에 실제로 들어온 문자열을 꺼내기 위함
            val context = LocalContext.current // Compose환경에서 Android Context 객체 얻음

            val rankings = remember { //sharedPreference에서 해당 카테고리 랭킹 목록을 한 번만 불러와서 저장
                RankingStore.loadRankingBYCategory(context, categoryId)
            }
            RankingScreen(//최종적으로 랭킹화면 호출
                ranking = rankings,
                //뒤로가기 버튼 누를시 작동 - 보통 바로 이전 화면에 가고싶을 떄 사용
                onBack = { navController.popBackStack() }
            )
        }

        //오답노트 화면
        composable(
            route = Screen.WrongNote.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            //어떤 카테고리의 오답노트인지
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            //선택된 닉네임 상태를 기억하기 위함 - 상태가 유지되야하기 때문
            var selectedName by remember { mutableStateOf<String?>(null) }
            //왼쪽에 표시할 닉네임 목록
            val nicknames = WrongAnswerStore.getNicknamesByCategory(categoryId)
            //오른쪽에 표시할 오답 리스트
            val wrongAnswers = WrongAnswerStore.getWrongAnswers(
                categoryId = categoryId,
                userName = selectedName
            )
            WrongNoteScreen( // 오답노트 화면 호출
                wrongAnswer = wrongAnswers,
                nicknames = nicknames,
                selectedName = selectedName,
                //닉네임버튼 누를시 실행
                onSelectedName = { name ->
                    selectedName = name
                },
                onDeleteWrongAnswer = { item ->
                    WrongAnswerStore.removeWrongAnswer(item)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        //결과 화면
        composable(
            route = Screen.Result.route,
            arguments = listOf( //결과화면은 퀴즈화면에서 누적된 점수, 총문제수와 카테고리 iD를 가져와야함
                navArgument(("categoryId")) { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            val context = LocalContext.current

            ResultScreen(
                score = score,
                total = total,
                categoryId,
                onShowWrongNote = {
                    //결과화면 -> 오답노트화면
                    navController.navigate(
                        Screen.WrongNote.createRoute(categoryId)
                    )
                },
                //결과화면 -> 메인화면
                onGoHome = {
                    navController.popBackStack( //뒤로가긴하는데,
                        route = Screen.Category.route, //루트를 카테고리 화면으로 지정
                        inclusive = false //카테고리만 남기고 그 위 화면들은 제거 ( 퀴즈화면같은것들 )
                    )
                },
                //결과화면 -> 랭킹화면 / 이름 넘겨줘야함
                onSaveRanking = { name ->
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) //날짜를 저장하기위한 상수 지정
                        .format(Date())
                    val item = Ranking(
                        name = name,
                        score = score,
                        total = total,
                        categoryId = categoryId,
                        date = currentDate      //날짜 추가
                    )
                    //랭킹에 위의 정보들 저장
                    RankingStore.addRanking(context, item)
                    //이름 가져온김에 오답노트 정보에도 이름 추가
                    WrongAnswerStore.setUserNameForAll(name) //오답에도 닉네임 채워주기
                }
            )
        }

        //퀴즈 화면
        composable(
            route = Screen.Quiz.route,   // "quiz/{categoryId}"
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            //QuizScreen 호출
            QuizScreen(
                categoryId = categoryId,
                onGoToMain = { navController.popBackStack() }, // <<< 뒤로가기 기능
                //퀴즈화면 -> 결과화면 // 점수와 총개수 넘겨줘야함
                onQuizFinished = { score, total ->
                    navController.navigate(
                        Screen.Result.createRoute(categoryId, score, total)
                    )
                }
            )
        }
    }
}
