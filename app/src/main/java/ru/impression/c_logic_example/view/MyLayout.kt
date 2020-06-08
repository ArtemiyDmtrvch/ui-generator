package ru.impression.c_logic_example.view

import android.widget.FrameLayout
import ru.impression.c_logic_annotations.MakeComponent
import ru.impression.c_logic_annotations.SharedViewModel
import ru.impression.c_logic_base.ComponentScheme
import ru.impression.c_logic_base.ComponentViewModel
import ru.impression.c_logic_example.databinding.MyLayoutBinding

@MakeComponent
class MyLayout : ComponentScheme<FrameLayout, MyLayoutViewModel>({ MyLayoutBinding::class })

@SharedViewModel
class MyLayoutViewModel : ComponentViewModel() {

    var text by state<String?>(null)
}