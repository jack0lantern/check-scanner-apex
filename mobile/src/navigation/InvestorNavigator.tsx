import React from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import type { InvestorStackParamList } from './types'
import { InputScreen } from '../screens/investor/InputScreen'
import { CameraScreen } from '../screens/investor/CameraScreen'
import { ReviewScreen } from '../screens/investor/ReviewScreen'
import { StatusScreen } from '../screens/investor/StatusScreen'
import { Colors } from '../theme/colors'

const Stack = createNativeStackNavigator<InvestorStackParamList>()

export function InvestorNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: Colors.bgElevated },
        headerTintColor: Colors.text,
        headerTitleStyle: { fontWeight: '600' },
      }}
    >
      <Stack.Screen
        name="Input"
        component={InputScreen}
        options={{ title: 'New Deposit' }}
      />
      <Stack.Screen
        name="CameraFront"
        component={CameraScreen}
        options={{ title: 'Front of Check' }}
      />
      <Stack.Screen
        name="CameraBack"
        component={CameraScreen}
        options={{ title: 'Back of Check' }}
      />
      <Stack.Screen
        name="Review"
        component={ReviewScreen}
        options={{ title: 'Review Deposit' }}
      />
      <Stack.Screen
        name="Status"
        component={StatusScreen}
        options={{ title: 'Deposit Status', headerBackVisible: false }}
      />
    </Stack.Navigator>
  )
}
