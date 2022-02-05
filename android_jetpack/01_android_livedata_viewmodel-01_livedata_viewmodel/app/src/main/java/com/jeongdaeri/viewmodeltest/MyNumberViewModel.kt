package com.jeongdaeri.viewmodeltest

import android.util.Log
import android.widget.Switch
import androidx.lifecycle.*
import com.jeongdaeri.viewmodeltest.MainActivity.Companion.TAG
import com.jeongdaeri.viewmodeltest.NumberActionType.*


enum class NumberActionType {
    PLUS, MINUS
}

// 데이터의 변경
// 뷰모델은 데이터의 변경사항을 알려주는 라이브 데이터를 가지고 있고
class MyNumberViewModel : ViewModel(){

    // 값이 변경될 것이기 때문에 var
    var value = 0


    // 내부에서 설정하는 자료형은 뮤터블로
    // 변경가능하도록 설정
    private val _currentValue = MutableLiveData<Int>()

    /*
    // 변경되지 않는 데이터를 가져 올때 이름을 _ 언더스코어 없이 설정
    // 공개적으로 가져오는 변수는 private 이 아닌 퍼블릭으로 외부에서도 접근가능하도록 설정
    // 하지만 값을 직접 라이브데이터에 접근하지 않고 뷰모델을 통해 가져올수 있도록 설정
    val currentValue : LiveData<Int>
        get() = _currentValue
    */

    // backing field 대신에  observe 함수를  ViewModel에 위치시키면  backing field를 사용할 필요가 없습니다.
    // 더 나아가 holder pattern을 쓰면 LiveData도 캡슐화가 가능합니다.
    fun observe(owner: LifecycleOwner, onChange: (Int) -> Unit) {
        _currentValue.observe(owner, Observer(onChange))
    }

    // 초기값 설정하기
    init {
        _currentValue.value = 0
    }

    // 라이브 데이터 종류 크게 2가지
    // 코틀린에서 콜렉션 종류 고정, mutable 수정가능한 녀석
    // wrapping 되어 있음 -
    // lazy 생성자를 통해서 나중에 값이 설정된다고 설정

    // 뷰모델이 가지고 있는 값을 변경하는 메소드
    fun updateValue(actionType: NumberActionType, input: Int){
        when (actionType) {
            PLUS ->
                _currentValue.value = _currentValue.value?.plus(input)
            MINUS ->
                _currentValue.value = _currentValue.value?.minus(input)
        }
    }

}
