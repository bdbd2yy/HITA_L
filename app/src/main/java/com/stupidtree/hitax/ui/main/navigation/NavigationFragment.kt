package com.stupidtree.hitax.ui.main.navigation

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.model.eas.EASToken
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.databinding.FragmentNavigationBinding
import com.stupidtree.style.base.BaseFragment
import com.stupidtree.hitax.ui.eas.classroom.EmptyClassroomActivity
import com.stupidtree.hitax.ui.eas.exam.ExamActivity
import com.stupidtree.hitax.ui.eas.imp.ImportTimetableActivity
import com.stupidtree.hitax.ui.eas.login.EASWebLoginActivity
import com.stupidtree.hitax.ui.eas.score.ScoreInquiryActivity
import com.stupidtree.hitax.utils.ActivityUtils
import com.stupidtree.style.widgets.PopUpText

class NavigationFragment : BaseFragment<NavigationViewModel, FragmentNavigationBinding>() {
    private var pendingAfterLogin: (() -> Unit)? = null
    private val easLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingAfterLogin?.invoke()
            refreshAccountEntry()
        }
        pendingAfterLogin = null
    }

    override fun getViewModelClass(): Class<NavigationViewModel> {
        return NavigationViewModel::class.java
    }

    override fun initViews(view: View) {
        viewModel.recentTimetableLiveData.observe(this) {
            if (it == null) {
                binding?.recentSubtitle?.setText(R.string.none)
            } else {
                binding?.recentSubtitle?.text = it.name
            }
        }
        viewModel.timetableCountLiveData.observe(this) {
            if (it == 0) {
                binding?.timetableSubtitle?.setText(R.string.no_timetable)
            } else {
                binding?.timetableSubtitle?.text = getString(R.string.timetable_count_format, it)
            }

        }
        viewModel.unreadMessageLiveData.observe(this) {
            binding?.messageNum?.visibility = View.GONE
        }
        binding?.cardTimetable?.setOnClickListener {
            ActivityUtils.startTimetableManager(requireContext())
        }
        binding?.cardRecentTimetable?.setOnClickListener {
            viewModel.recentTimetableLiveData.value?.let {
                ActivityUtils.startTimetableDetailActivity(requireContext(), it.id)
            }
        }
        binding?.cardImport?.setOnClickListener {
            launchEasLoginIfNeeded {
                ActivityUtils.startActivity(requireContext(), ImportTimetableActivity::class.java)
            }
        }
        binding?.cardEmptyClassroom?.setOnClickListener {
            launchEasLoginIfNeeded {
                ActivityUtils.startActivity(requireContext(), EmptyClassroomActivity::class.java)
            }
        }
        binding?.cardScores?.setOnClickListener {
            launchEasLoginIfNeeded {
                ActivityUtils.startActivity(requireContext(), ScoreInquiryActivity::class.java)
            }
        }
        binding?.cardSubjects?.setOnClickListener {
            launchEasLoginIfNeeded {
                ActivityUtils.startActivity(requireContext(), ExamActivity::class.java)
            }
        }
        binding?.search?.setOnClickListener {
            binding?.search?.let { v ->
                ActivityUtils.startSearchActivity(requireActivity(), v)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRefresh()
        refreshAccountEntry()
    }

    override fun onResume() {
        super.onResume()
        refreshAccountEntry()
    }

    private fun refreshAccountEntry() {
        val token = activity?.application?.let { EASRepository.getInstance(it).getEasToken() }
        if (token?.isLogin() == true) {
            val accountName = resolveEasAccountName(token)
            binding?.nickname?.setText(R.string.jw_login_connected_title)
            binding?.username?.text = getString(R.string.jw_login_status_with_student_id, accountName)
            binding?.avatar?.setImageResource(R.drawable.place_holder_avatar)
            binding?.userStatusDot?.visibility = View.VISIBLE
            binding?.userActionIcon?.setImageResource(R.drawable.ic_baseline_exit_to_app_24)
            binding?.userActionIcon?.setOnClickListener {
                PopUpText().setTitle(R.string.menu_logout_jw)
                    .setOnConfirmListener(object : PopUpText.OnConfirmListener {
                        override fun OnConfirm() {
                            activity?.application?.let {
                                EASRepository.getInstance(it).logout()
                                refreshAccountEntry()
                            }
                        }
                    }).show(parentFragmentManager, "logout")
            }
            binding?.userCard?.setOnClickListener(null)
        } else {
            binding?.nickname?.setText(R.string.jw_login_entry_title)
            binding?.username?.setText(R.string.jw_login_entry_subtitle)
            binding?.avatar?.setImageResource(R.drawable.place_holder_avatar)
            binding?.userStatusDot?.visibility = View.GONE
            binding?.userActionIcon?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
            binding?.userActionIcon?.setOnClickListener {
                launchEasLoginIfNeeded {
                    refreshAccountEntry()
                }
            }
            binding?.userCard?.setOnClickListener {
                launchEasLoginIfNeeded {
                    refreshAccountEntry()
                }
            }
        }
    }

    private fun resolveEasAccountName(token: EASToken): String {
        return when {
            !token.stuId.isNullOrBlank() -> token.stuId!!.trim()
            else -> "--"
        }
    }

    private fun launchEasLoginIfNeeded(onLoggedIn: () -> Unit) {
        val token = activity?.application?.let { EASRepository.getInstance(it).getEasToken() }
        if (token?.isLogin() == true) {
            onLoggedIn()
            return
        }
        pendingAfterLogin = onLoggedIn
        easLoginLauncher.launch(Intent(requireContext(), EASWebLoginActivity::class.java))
    }


    override fun initViewBinding(): FragmentNavigationBinding {
        return FragmentNavigationBinding.inflate(layoutInflater)
    }
}
