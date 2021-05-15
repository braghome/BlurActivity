/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background.legacy

import android.content.Intent
import android.os.Bundle
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.example.background.BlurViewModel
import com.example.background.KEY_IMAGE_URI
import com.example.background.R
import com.example.background.databinding.ActivityBlurBinding
import com.example.background.databinding.ActivityBlurBinding.inflate

class LegacyBlurActivity : AppCompatActivity() {

    private lateinit var viewModel: BlurViewModel
    private lateinit var binding: ActivityBlurBinding
    private fun workInfosObserver() = Observer<List<WorkInfo>> { listOfWorkInfo ->
        if (listOfWorkInfo.isNullOrEmpty()) {
            return@Observer
        }
        val workInfo = listOfWorkInfo[0]
        if (workInfo.state.isFinished) {
            showWorkFinished()
            val outputImageUri = workInfo.outputData.getString(KEY_IMAGE_URI)
            if (outputImageUri.isNullOrEmpty().not()) {
                viewModel.setOutputUri(outputImageUri as String)
                binding.seeFileButton.visibility = VISIBLE
            }
        } else {
            showWorkInProgress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(BlurViewModel::class.java)

        viewModel.outWorkInfos.observe(this, workInfosObserver())

        // Image uri should be stored in the ViewModel; put it there then display
        val imageUriExtra = intent.getStringExtra(KEY_IMAGE_URI)
        viewModel.setImageUri(imageUriExtra)
        viewModel.imageUri?.let { imageUri ->
            Glide.with(this).load(imageUri).into(binding.imageView)
        }

        binding.goButton.setOnClickListener { viewModel.applyBlur(blurLevel) }
        binding.seeFileButton.setOnClickListener {
            viewModel.outputUri?.let { currentUri ->
                val actionView = Intent(Intent.ACTION_VIEW, currentUri)
                actionView.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                actionView.resolveActivity(packageManager)?.run {
                    startActivity(actionView)
                }
            }
        }
        binding.cancelButton.setOnClickListener { viewModel.cancelWork() }
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private fun showWorkInProgress() {
        with(binding) {
            progressBar.visibility = VISIBLE
            cancelButton.visibility = VISIBLE
            goButton.visibility = GONE
            seeFileButton.visibility = GONE
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private fun showWorkFinished() {
        with(binding) {
            progressBar.visibility = GONE
            cancelButton.visibility = GONE
            goButton.visibility = VISIBLE
        }
    }

    private val blurLevel: Int
        get() =
            when (binding.radioBlurGroup.checkedRadioButtonId) {
                R.id.radio_blur_lv_1 -> 1
                R.id.radio_blur_lv_2 -> 2
                R.id.radio_blur_lv_3 -> 3
                else -> 1
            }
}
