package com.github.canny1913.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.discord.utilities.dimen.DimenUtils
import com.discord.utilities.mg_recycler.MGRecyclerAdapterSimple
import com.discord.utilities.mg_recycler.MGRecyclerDataPayload
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder
import com.discord.utilities.textprocessing.AstRenderer
import com.discord.utilities.textprocessing.MessageRenderContext
import com.discord.utilities.view.rounded.RoundedRelativeLayout
import com.discord.utilities.view.text.SimpleDraweeSpanTextView
import com.facebook.drawee.view.SimpleDraweeView
import com.github.canny1913.R
import com.github.canny1913.adapter.items.OnboardingItemHeader
import com.github.canny1913.adapter.items.OnboardingItemPromptOption
import com.github.canny1913.adapter.items.OnboardingItemTerm
import com.github.canny1913.adapter.items.OnboardingItemType
import com.github.canny1913.databinding.OnboardingHeaderBinding
import com.github.canny1913.databinding.OnboardingPromptOptionBinding
import com.github.canny1913.databinding.OnboardingTermBinding
import com.github.canny1913.databinding.OnboardingTermHeaderBinding


class OnboardingAdapter(
    recyclerView: RecyclerView,
    var onOptionClicked: (Int, Long) -> Unit,
) : MGRecyclerAdapterSimple<MGRecyclerDataPayload>(recyclerView, true) {
    class HeaderItemHolder(adapter: OnboardingAdapter) :
        MGRecyclerViewHolder<OnboardingAdapter, MGRecyclerDataPayload?>(R.layout.onboarding_header, adapter) {
        val binding: OnboardingHeaderBinding

        init {
            val promptIndexView: TextView = itemView.findViewById(R.id.onboarding_prompt_index)!!
            val promptDotView: TextView = itemView.findViewById(R.id.weird_dot)!!
            val promptRequiredView: TextView = itemView.findViewById(R.id.onboarding_prompt_required)!!
            val promptTextView: TextView = itemView.findViewById(R.id.onboarding_prompt_text)!!
            binding = OnboardingHeaderBinding(
                itemView as LinearLayout,
                promptIndexView,
                promptRequiredView,
                promptDotView,
                promptTextView
            )
        }

        override fun onConfigure(type: Int, data: MGRecyclerDataPayload?) {
            val headerData = data as OnboardingItemHeader
            super.onConfigure(type, data)
            binding.indexView.text =
                adapter.context.getString(R.string.question_index, headerData.currentQuestion + 1, headerData.questionCount)
            binding.textView.text = headerData.question
            binding.dotView.visibility = if (headerData.required) View.VISIBLE else View.GONE
            binding.requiredView.visibility = if (headerData.required) View.VISIBLE else View.GONE
        }
    }
    class PromptOptionItemHolder(adapter: OnboardingAdapter) :
        MGRecyclerViewHolder<OnboardingAdapter, MGRecyclerDataPayload?>(R.layout.onboarding_prompt_option, adapter) {
        val binding: OnboardingPromptOptionBinding

        init {
            val emojiView: SimpleDraweeView = itemView.findViewById(R.id.onboarding_prompt_option_icon)!!
            val clickedView: View = itemView.findViewById(R.id.prompt_option_clicked)!!
            val optionTitleView: TextView = itemView.findViewById(R.id.onboarding_prompt_option_title)!!
            val optionDescriptionView: TextView = itemView.findViewById(R.id.onboarding_prompt_option_description)!!
            binding = OnboardingPromptOptionBinding(
                rootView = itemView as ConstraintLayout,
                clickedView,
                emojiView,
                optionTitleView,
                optionDescriptionView
            )
        }

        override fun onConfigure(type: Int, data: MGRecyclerDataPayload?) {
            val optionData = data as OnboardingItemPromptOption
            super.onConfigure(type, data)
            val emojiUri = optionData.emoji.getImageUri(true, 512, adapter.context)
            binding.emojiView.setImageURI(emojiUri)
            itemView.setOnClickListener { adapter.onOptionClicked(optionData.promptIndex, optionData.id) }
            binding.clickedView.visibility = if (optionData.selected) View.VISIBLE else View.GONE
            binding.optionTitleView.text = optionData.title
            binding.optionDescriptionView.text = optionData.description
            binding.optionDescriptionView.visibility = if (optionData.description.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    class TermHeaderItemHolder(adapter: OnboardingAdapter) :
        MGRecyclerViewHolder<OnboardingAdapter, MGRecyclerDataPayload?>(R.layout.onboarding_term_header, adapter) {
        val binding: OnboardingTermHeaderBinding

        init {
            val titleView: TextView = itemView.findViewById(R.id.onboarding_header_term_title)!!
            val descView: TextView = itemView.findViewById(R.id.onboarding_header_term_desc)!!
            binding = OnboardingTermHeaderBinding(
                itemView as LinearLayout,
                titleView,
                descView
            )
        }

    }
    class TermItemHolder(adapter: OnboardingAdapter) :
        MGRecyclerViewHolder<OnboardingAdapter, MGRecyclerDataPayload?>(R.layout.onboarding_term, adapter) {
        val binding: OnboardingTermBinding

        init {
            val termView: SimpleDraweeSpanTextView = itemView.findViewById(R.id.onboarding_term_description)!!
            val dividerView: View = itemView.findViewById(R.id.onboarding_term_divider)!!
            val indexView: TextView = itemView.findViewById(R.id.onboarding_term_index)!!
            binding = OnboardingTermBinding(
                itemView as RoundedRelativeLayout,
                termView,
                dividerView,
                indexView
            )
        }

        override fun onConfigure(type: Int, data: MGRecyclerDataPayload?) {
            val termData = data as OnboardingItemTerm
            super.onConfigure(type, data)

            binding.indexView.text = itemView.context.getString(R.string.term_index, termData.index + 1)
            this.binding.termView.setDraweeSpanStringBuilder(
                AstRenderer.render(
                    termData.ast,
                    MessageRenderContext(
                        itemView.context,
                        0L,
                        termData.allowAnimatedEmojis,
                        null,
                        termData.channelNames,
                        termData.roles,
                    )
                )
            )
            binding.dividerView.visibility = if (!termData.isLastItem) View.VISIBLE else View.GONE
            val fDpToPixels = DimenUtils.dpToPixels(4).toFloat()
            if (termData.isFirstItem) {
                binding.rootView.updateTopLeftRadius(fDpToPixels)
                binding.rootView.updateTopRightRadius(fDpToPixels)
            }
            if (termData.isLastItem) {
                binding.rootView.updateBottomLeftRadius(fDpToPixels)
                binding.rootView.updateBottomRightRadius(fDpToPixels)
            }
        }
    }

    override fun onCreateViewHolder(
        root: ViewGroup, dataType: Int
    ): MGRecyclerViewHolder<*, MGRecyclerDataPayload?> {
        return when (dataType) {
            OnboardingItemType.HEADER.type -> HeaderItemHolder(this)
            OnboardingItemType.PROMPT_MULTIPLE_CHOICE.type -> PromptOptionItemHolder(this)
            OnboardingItemType.VERIFICATION_TERM.type -> TermItemHolder(this)
            OnboardingItemType.VERIFICATION_TERM_HEADER.type -> TermHeaderItemHolder(this)
            else -> throw invalidViewTypeException(dataType)
        }
    }
}
