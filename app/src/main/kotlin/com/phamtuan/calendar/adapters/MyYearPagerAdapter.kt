package com.phamtuan.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import com.phamtuan.calendar.fragments.YearFragment
import com.phamtuan.calendar.helpers.YEAR_LABEL
import com.phamtuan.calendar.interfaces.NavigationListener

class MyYearPagerAdapter(fm: FragmentManager, val mYears: List<Int>, val mListener: NavigationListener) : FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<YearFragment>()

    override fun getCount() = mYears.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val year = mYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle
        fragment.mListener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun refreshEvents(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateEvents()
        }
    }
}
