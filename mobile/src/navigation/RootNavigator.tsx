import React from 'react'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { StatusBar } from 'expo-status-bar'
import type { RootStackParamList } from './types'
import { WelcomeScreen } from '../screens/WelcomeScreen'
import { InvestorNavigator } from './InvestorNavigator'
import { OperatorNavigator } from './OperatorNavigator'
import { Colors } from '../theme/colors'

const Stack = createNativeStackNavigator<RootStackParamList>()

export function RootNavigator() {
  return (
    <NavigationContainer
      theme={{
        dark: true,
        colors: {
          primary: Colors.accent,
          background: Colors.bg,
          card: Colors.bgElevated,
          text: Colors.text,
          border: Colors.border,
          notification: Colors.accent,
        },
      }}
    >
      <StatusBar style="light" />
      <Stack.Navigator
        screenOptions={{ headerShown: false }}
        initialRouteName="Welcome"
      >
        <Stack.Screen name="Welcome" component={WelcomeScreen} />
        <Stack.Screen name="InvestorStack" component={InvestorNavigator} />
        <Stack.Screen name="OperatorStack" component={OperatorNavigator} />
      </Stack.Navigator>
    </NavigationContainer>
  )
}
