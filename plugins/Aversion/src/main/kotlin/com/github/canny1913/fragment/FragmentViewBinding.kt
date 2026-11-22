package com.github.canny1913.fragment

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KProperty


class FragmentViewBinding<T: ViewBinding>(
    val fragment: Fragment,
    val onDestroy: (T) -> (Unit),
    val factory: (View) -> (T)
) {
    operator fun getValue(fragment: Fragment, property: KProperty<*>): T {
        if (binding != null) {
            return binding!!
        } else {
            addOnDestroyObserver()
            val lifecycleOwner = fragment.viewLifecycleOwner
            val lifecycle = lifecycleOwner.lifecycle
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                this.binding = factory(fragment.requireView())
                return binding!!
            } else {
                throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
            }
        }
    }
    private var binding: T? = null
    fun addOnDestroyObserver() {
        fragment.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onCreate(p0: LifecycleOwner) {
                fragment.mViewLifecycleOwnerLiveData.observe(this@FragmentViewBinding.fragment,
                    Observer { p0 ->
                        p0.lifecycle.addObserver( object: DefaultLifecycleObserver {
                            override fun onDestroy(p0: LifecycleOwner) {
                                binding?.let { this@FragmentViewBinding.onDestroy(it) }
                                binding = null
                            }
                            override fun onCreate(p0: LifecycleOwner) {}
                            override fun onPause(p0: LifecycleOwner) {}
                            override fun onResume(p0: LifecycleOwner) {}
                            override fun onStart(p0: LifecycleOwner) {}
                            override fun onStop(p0: LifecycleOwner) {}
                        })
                    })
            }
            override fun onDestroy(p0: LifecycleOwner) {}
            override fun onPause(p0: LifecycleOwner) {}
            override fun onResume(p0: LifecycleOwner) {}
            override fun onStart(p0: LifecycleOwner) {}
            override fun onStop(p0: LifecycleOwner) {}
        })
    }
}
fun <T: ViewBinding> Fragment.fragmentBinding(onDestroy: (T) -> Unit, factory: (View) -> (T)) = FragmentViewBinding(
    this,
    onDestroy,
    factory
)
fun <T: ViewBinding> Fragment.fragmentBinding(factory: (View) -> (T)) = fragmentBinding({}, factory)
