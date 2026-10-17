package com.github.canny1913

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.discord.app.AppFragment
import com.discord.utilities.dimen.DimenUtils
import com.github.canny1913.adapter.OnboardingAdapter
import com.github.canny1913.databinding.OnboardingFragmentBinding
import com.github.canny1913.fragment.fragmentBinding
import com.github.canny1913.viewmodel.OnboardingViewModel
import com.github.canny1913.viewmodel.OnboardingViewModel.Companion.loggerSubscribe
import com.github.canny1913.viewmodel.OnboardingViewModel.Companion.uiWithLifecycle
import com.github.canny1913.viewmodel.activityViewModels


class OnboardingFragment(
    val ctx: Context, val guildId: Long
) : AppFragment(R.layout.widget_onboarding) {

    lateinit var rulesAdapter: OnboardingAdapter
    val viewModel: OnboardingViewModel by activityViewModels {
        OnboardingViewModel.provideFactory(guildId)
    }

    val binding: OnboardingFragmentBinding by fragmentBinding { rootView ->
        OnboardingFragmentBinding(
            rootView as ConstraintLayout,
            toolbarView = rootView.findViewById(R.id.action_bar_toolbar),
            progressBarView = rootView.findViewById(R.id.progress_bar),
            recyclerView = rootView.findViewById(R.id.onboarding_recycler),
            scrollView = rootView.findViewById(R.id.onboarding_scrollview),
            dimmerView = rootView.findViewById(R.id.dimmer_view),
            nextGradientView = rootView.findViewById(R.id.onboarding_next_gradient),
            nextButtonView = rootView.findViewById(R.id.onboarding_next),
            roleTextView = rootView.findViewById(R.id.onboarding_role_text)
        )
    }
    override fun getContext(): Context {
        return ctx
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun getLayoutInflater(p0: Bundle?): LayoutInflater {
        return LayoutInflater.from(context)
    }

    override fun onCreateView(inflater: LayoutInflater, root: ViewGroup?, bundle: Bundle?): View? {
        return layoutInflater.inflate(R.layout.widget_onboarding, root, false)
    }

    override fun onViewBound(view: View?) {
        super.onViewBound(view)

        val recyclerView = binding.recyclerView
        rulesAdapter = OnboardingAdapter(
            recyclerView,
            onOptionClicked = viewModel::onOptionClick,
        )
        recyclerView.adapter = rulesAdapter

        // disable default animations to avoid 'blinking'
        val animator = recyclerView.itemAnimator as? SimpleItemAnimator
        animator?.let { it.supportsChangeAnimations = false }

        rulesAdapter.setOnUpdated { _, _ ->
            updateRoleText(viewModel.fetchSelectedRoles())
        }
        binding.nextButtonView.setOnClickListener { viewModel.onNextClick(binding.nextButtonView, binding.scrollView) }
        binding.toolbarView.setNavigationOnClickListener { viewModel.onBackPress() }
        this.setOnBackPressed(viewModel::onBackPress)
    }

    fun configureUI(viewState: OnboardingViewModel.ViewState) {
        if (viewState is OnboardingViewModel.ViewState.Completed) {
            requireActivity().finish()
        }
        if (viewState is OnboardingViewModel.ViewState.Invalid) {
            Toast.makeText(ctx, R.string.onboarding_load_fail, Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
        if (viewState is OnboardingViewModel.ViewState.Loading) {
            val nestedScrollView = binding.scrollView
            nestedScrollView.visibility = View.GONE
            binding.nextButtonView.visibility = View.GONE
            binding.dimmerView.setDimmed(true, true)
        }
        if (viewState is OnboardingViewModel.ViewState.Loaded) {
            val nestedScrollView = binding.scrollView
            nestedScrollView.visibility = View.VISIBLE
            // make sure we're using NestedScrollView.OnScrollChangeListener here
            // since the View version is API >23 only
            nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _, y, _, _ -> applyFadingGradient(v, y) }
            binding.nextButtonView.apply {
                val nextButtonTextId = if (viewState.isLast()) R.string.finish else R.string.next
                this.setText(context.getString(nextButtonTextId))
                this.setIsLoading(false)
                visibility = View.VISIBLE
                isEnabled = !viewState.disabled
                isClickable = !viewState.disabled
            }

            binding.dimmerView.setDimmed(false, false)
            updateProgressBar(viewState)
            rulesAdapter.setData(viewState.items)
        }
    }



    fun updateProgressBar(viewState: OnboardingViewModel.ViewState.Loaded) {
        val begin = binding.progressBarView.progress
        val end = (viewState.currentPrompt.toFloat() / viewState.prompts.size.toFloat() * 10000).toInt()
        val animator = ValueAnimator.ofInt(begin, end)

        animator.duration = 300
        animator.interpolator = DecelerateInterpolator(2.0f)

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            binding.progressBarView.progress = value
        }

        animator.start()
    }

    fun applyFadingGradient(v: View, scrollY: Int) {
        val scrollView = v as ViewGroup
        val scrollableY: Int = scrollView.getChildAt(0).measuredHeight - scrollView.measuredHeight
        val newHeightFactor = 1f - (scrollY.toFloat() / scrollableY)
        val newHeight = (newHeightFactor * DimenUtils.dpToPixels(16).toFloat()).toInt()
        val nextGradientView = binding.nextGradientView
        if (newHeight == 0) {
            nextGradientView.visibility = View.GONE
            return
        } else {
            nextGradientView.visibility = View.VISIBLE
        }
        nextGradientView.layoutParams = nextGradientView.layoutParams.apply { height = newHeight }
    }
    fun updateRoleText(roles: List<String>?) {
        if (roles.isNullOrEmpty()) {
            binding.roleTextView.visibility = View.GONE
            return
        }
        binding.roleTextView.visibility = View.VISIBLE
        val text = when (roles.size) {
            1 -> this.getString(R.string.one_role, roles[0])
            2 -> this.getString(R.string.two_role, roles[0], roles[1])
            3 -> this.getString(R.string.three_role, roles[0], roles[1], 1)
            else -> this.getString(R.string.more_role, roles[0], roles[1], roles.size - 2)
        }
        val spannableString = SpannableStringBuilder(text)
        // apply bold styling to role names
        roles.take(2).forEach { role ->
            val start = text.indexOf(role)
            val end = start + role.length
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.roleTextView.text = spannableString
    }
    override fun onViewBoundOrOnResume() {
        super.onViewBoundOrOnResume()
        viewModel.viewStateSubject.uiWithLifecycle(this).loggerSubscribe { configureUI(it!!) }
    }
}
fun NestedScrollView.isAtBottom(tolerance: Int = 8): Boolean {
    val child = getChildAt(0) ?: return true
    return this.scrollY >= child.measuredHeight - height - tolerance
}
